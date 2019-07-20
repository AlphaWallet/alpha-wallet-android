package io.stormbird.wallet.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.security.keystore.*;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SignTransactionDialog;
import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Set;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static io.stormbird.wallet.entity.ServiceErrorException.*;
import static io.stormbird.wallet.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

@TargetApi(23)
public class HDKeyService implements AuthenticationCallback, PinAuthenticationCallbackInterface
{
    private static final String TAG = "HDWallet";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String UNLOCK_KEY = "unlock_key";
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    public static final int TIME_BETWEEN_BACKUP_MILLIS = 1000*60*1; //TODO: RESTORE 30 DAYS. TESTING: 10 minutes  //1000 * 60 * 60 * 24 * 30; //30 days
    public static final int TIME_BETWEEN_BACKUP_WARNING_MILLIS = 1000*60*1; //TODO: RESTORE 30 DAYS. TESTING: 10 minutes  //1000 * 60 * 60 * 24 * 30; //30 days

    private enum Operation
    {
        CREATE_HD_KEY, UNLOCK_HD_KEY, FETCH_MNEMONIC, IMPORT_HD_KEY
    }

    private static final int DEFAULT_KEY_STRENGTH = 128;
    private final Activity context;

    private HDWallet currentWallet;
    private String currentKey;
    private SignTransactionDialog signDialog;
    private CreateWalletCallbackInterface callbackInterface;
    private ImportWalletCallback importCallback;

    public HDKeyService(Activity ctx)
    {
        System.loadLibrary("TrustWalletCore");
        context = ctx;
    }

    public String createNewHDKey(CreateWalletCallbackInterface callback)
    {
        currentWallet = new HDWallet(DEFAULT_KEY_STRENGTH, "key1");
        String mnemonic = currentWallet.mnemonic();
        PrivateKey pk = currentWallet.getKeyForCoin(CoinType.ETHEREUM);
        currentKey =  CoinType.ETHEREUM.deriveAddress(pk);
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        checkAuthentication(Operation.CREATE_HD_KEY);
        return currentKey;
    }

    private void createHDKey2()
    {
        boolean success = storeHDWallet(currentWallet);
        PrivateKey pk = currentWallet.getKeyForCoin(CoinType.ETHEREUM);
        String address = CoinType.ETHEREUM.deriveAddress(pk);
        if (!success) address = ZERO_ADDRESS;
        callbackInterface.HDKeyCreated(address, context);
    }

    public void getMnemonic(String address, CreateWalletCallbackInterface callback)
    {
        currentKey = address;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        checkAuthentication(Operation.FETCH_MNEMONIC);
    }

    public void importHDKey(String seedPhrase, ImportWalletCallback callback)
    {
        //cursory check for valid key import
        if (!HDWallet.isValid(seedPhrase))
        {
            callback.WalletValidated(null);
        }
        else
        {
            currentWallet = new HDWallet(seedPhrase, "key1");
            PrivateKey pk = currentWallet.getKeyForCoin(CoinType.ETHEREUM);
            currentKey = CoinType.ETHEREUM.deriveAddress(pk);
            importCallback = callback;
            checkAuthentication(Operation.IMPORT_HD_KEY);
        }
    }

    private void importHDKey2()
    {
        if (storeHDWallet(currentWallet))
        {
            importCallback.WalletValidated(currentKey);
        }
        else
        {
            importCallback.WalletValidated(null);
        }
    }

    public byte[] signData(String key, byte[] data)
    {
        //unlock
        return null;
    }

    private synchronized byte[] signData(String data, String keyAddress) throws ServiceErrorException
    {
        KeyStore keyStore;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(keyAddress)) return null;
            String encryptedDataFilePath = getFilePath(context, keyAddress + "hd");
            SecretKey secretKey = (SecretKey) keyStore.getKey(keyAddress, null);
            if (secretKey == null)
            {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
                if (!fileExists)
                {
                    return null;/* file also not there, fine then */
                }
                throw new ServiceErrorException(
                        KEY_IS_GONE,
                        "file is present but the key is gone: " + keyAddress);
            }

            boolean ivExists = new File(getFilePath(context, currentKey + "iv")).exists();
            byte[] iv =  null;

            if (ivExists) iv = readBytesFromFile(getFilePath(context, currentKey + "iv"));
            if (iv == null || iv.length == 0)
            {
                throw new NullPointerException("iv is missing for " + currentKey + "iv");
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] mnemonicBytes = readBytesFromStream(cipherInputStream);
            String mnemonic = new String(mnemonicBytes);
            return null;
        }
        catch (InvalidKeyException e)
        {
            if (e instanceof UserNotAuthenticatedException)
            {
                //				showAuthenticationScreen(context, requestCode);
                throw new ServiceErrorException(USER_NOT_AUTHENTICATED);
            }
            else
            {
                throw new ServiceErrorException(INVALID_KEY);
            }
        }
        catch (IOException | CertificateException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e)
        {
            throw new ServiceErrorException(KEY_STORE_ERROR);
        }
    }

    private synchronized void unpackMnemonic()
    {
        KeyStore keyStore;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            Enumeration<String> ksList = keyStore.aliases();
            if (!keyStore.containsAlias(currentKey)) { callbackInterface.tryAgain(); return; }
            String encryptedDataFilePath = getFilePath(context, currentKey + "hd");
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentKey, null);
            if (secretKey == null)
            {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
                if (!fileExists)
                {
                    callbackInterface.tryAgain();
                    return;
                }
            }

            boolean ivExists = new File(getFilePath(context, currentKey + "iv")).exists();
            byte[] iv =  null;

            if (ivExists) iv = readBytesFromFile(getFilePath(context, currentKey + "iv"));
            if (iv == null || iv.length == 0)
            {
                callbackInterface.tryAgain();
                return;
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] mnemonicBytes = readBytesFromStream(cipherInputStream);
            String mnemonic = new String(mnemonicBytes);
            callbackInterface.FetchMnemonic(mnemonic);
            return;
        }
        catch (InvalidKeyException e)
        {
            if (e instanceof UserNotAuthenticatedException)
            {
                callbackInterface.tryAgain();
            }
            else
            {
                callbackInterface.tryAgain();
                //throw new ServiceErrorException(INVALID_KEY);
            }
        }
        catch (IOException | CertificateException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e)
        {
            callbackInterface.tryAgain();
            //throw new ServiceErrorException(KEY_STORE_ERROR);
        }
    }

    private synchronized void deleteKey(KeyStore keyStore, String keyAddr) {
        try {
            File encrypted = new File(getFilePath(context, keyAddr + "hd"));
            File iv = new File(getFilePath(context, keyAddr + "iv"));
            if (encrypted.exists()) encrypted.delete();
            if (iv.exists()) iv.delete();
            keyStore.deleteEntry(keyAddr);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean storeHDWallet(HDWallet wallet)
    {
        KeyStore keyStore;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            PrivateKey pk = currentWallet.getKeyForCoin(CoinType.ETHEREUM);
            currentKey = CoinType.ETHEREUM.deriveAddress(pk);

            if (keyStore.containsAlias(currentKey)) //re-import existing key.
            {
                deleteKey(keyStore, currentKey);
            }

            KeyGenerator keyGenerator = getMaxSecurityKeyGenerator();

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            String path = getFilePath(context, currentKey + "hd");
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = inCipher.getIV();
            String ivPath = getFilePath(context, currentKey + "iv");
            boolean success = writeBytesToFile(ivPath, iv);
            if (!success) {
                keyStore.deleteEntry(currentKey + "iv");
                throw new ServiceErrorException(
                        ServiceErrorException.FAIL_TO_SAVE_IV_FILE,
                        "Failed to saveTokens the iv file for: " + currentKey + "iv");
            }

            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(path),
                    inCipher))
            {
                cipherOutputStream.write(wallet.mnemonic().getBytes());
            }
            catch (Exception ex)
            {
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_STORE_ERROR,
                        "Failed to saveTokens the file for: " + currentKey);
            }

            SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKey.getAlgorithm(), ANDROID_KEY_STORE);
            KeyInfo keyInfo = (KeyInfo)factory.getKeySpec(secretKey, KeyInfo.class);

            //TODO: store this in the key itself
            if (keyInfo.isInsideSecureHardware())
            {
                System.out.println("Using hardware protected key");
            }
            else
            {
                System.out.println("Not using hardware protected key");
            }

            return true;
        }
        catch (UserNotAuthenticatedException e)
        {

        }
        catch (Exception ex)
        {
            Log.d(TAG, "Key store error", ex);
            //throw new ServiceErrorException(KEY_STORE_ERROR);
        }

        return false;
    }

    private KeyGenerator getMaxSecurityKeyGenerator()
    {
        KeyGenerator keyGenerator;

        try
        {
            keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE);
        }
        catch (NoSuchAlgorithmException|NoSuchProviderException ex)
        {
            ex.printStackTrace();
            return null;
        }

        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tryInitStrongBoxKey(keyGenerator))
            {
                Log.d(TAG, "Using Strongbox");
            }
            else
            {
                //fallback to non Strongbox
                tryInitUserAuthenticatedKey(keyGenerator);
            }
        }
        catch (InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
            keyGenerator = null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            keyGenerator = null;
        }

        return keyGenerator;
    }

    private boolean tryInitStrongBoxKey(KeyGenerator keyGenerator) throws InvalidAlgorithmParameterException
    {
        try
        {
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    currentKey,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                      .setBlockModes(BLOCK_MODE)
                                      .setKeySize(256)
                                      .setUserAuthenticationRequired(true)
                                      .setIsStrongBoxBacked(true)
                                      .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                                      .setRandomizedEncryptionRequired(true)
                                      .setEncryptionPaddings(PADDING)
                                      .build());

            keyGenerator.generateKey();
        }
        catch (StrongBoxUnavailableException e)
        {
            Log.d(TAG, "Android 9 device doesn't have StrongBox");
            return false;
        }

        return true;
    }

    private void tryInitUserAuthenticatedKey(KeyGenerator keyGenerator) throws InvalidAlgorithmParameterException
    {
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                currentKey,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                  .setBlockModes(BLOCK_MODE)
                                  .setKeySize(256)
                                  .setUserAuthenticationRequired(true)
                                  .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                                  .setRandomizedEncryptionRequired(true)
                                  .setEncryptionPaddings(PADDING)
                                  .build());
    }

    private void checkAuthentication(Operation operation)
    {
        signDialog = new SignTransactionDialog(context, operation.ordinal());
        signDialog.setCanceledOnTouchOutside(false);
        signDialog.setCancelListener(v -> { authenticateFail("Cancelled", AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, operation.ordinal()); });
        signDialog.show();
        signDialog.getFingerprintAuthorisation(this);
    }

    private synchronized static String getFilePath(Context context, String fileName) {
        return new File(context.getFilesDir(), fileName).getAbsolutePath();
    }

    private static boolean writeBytesToFile(String path, byte[] data) {
        FileOutputStream fos = null;
        try {
            File file = new File(path);
            fos = new FileOutputStream(file);
            // Writes bytes from the specified byte array to this file output stream
            fos.write(data);
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while writing file " + ioe);
        } finally {
            // close the streams using close method
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
        return false;
    }

    private static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        FileInputStream fin;
        try {
            File file = new File(path);
            fin = new FileInputStream(file);
            bytes = readBytesFromStream(fin);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }


    private static byte[] readBytesFromStream(InputStream in) {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                byteBuffer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    @Override
    public void CompleteAuthentication(int callbackId)
    {
        authenticatePass(callbackId);
    }

    @Override
    public void FailedAuthentication(int taskCode)
    {
        authenticateFail("Authentication fail", AuthenticationFailType.PIN_FAILED, taskCode);
    }

    @Override
    public void authenticatePass(int callbackId)
    {
        signDialog.dismiss();
        //resume key operation
        Operation operation = Operation.values()[callbackId];
        switch (operation)
        {
            case CREATE_HD_KEY:
                createHDKey2();
                break;
            case UNLOCK_HD_KEY:
                break;
            case FETCH_MNEMONIC:
                unpackMnemonic();
                break;
            case IMPORT_HD_KEY:
                importHDKey2();
                break;
            default:
                break;
        }
    }

    @Override
    public void authenticateFail(String fail, AuthenticationFailType failType, int callbackId)
    {
        System.out.println("AUTH FAIL: " + failType.ordinal());
        Vibrator vb;

        switch (failType)
        {
            case AUTHENTICATION_DIALOG_CANCELLED:
                callbackInterface.cancelAuthentication();
                if (signDialog != null && signDialog.isShowing()) signDialog.dismiss();
                break;
            case FINGERPRINT_NOT_VALIDATED:
                vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vb != null && vb.hasVibrator())
                {
                    VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                    vb.vibrate(vibe);
                }
                Toast.makeText(context, "Fingerprint authentication failed", Toast.LENGTH_SHORT).show();
                break;
            case PIN_FAILED:
                vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vb != null && vb.hasVibrator())
                {
                    VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                    vb.vibrate(vibe);
                }
                showTryAgain(callbackId);
                break;
            case DEVICE_NOT_SECURE:
                showInsecure(callbackId);
                if (signDialog != null && signDialog.isShowing()) signDialog.dismiss();
                break;
        }

        if (context == null || context.isDestroyed())
        {
            callbackInterface.cancelAuthentication();
        }
    }

    private void showTryAgain(int callbackId)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(context);
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.authentication_failed);
        dialog.setMessage(R.string.authentication_failed_message);
        dialog.setButtonText(R.string.action_try_again);
        dialog.setButtonListener(v -> {
            checkAuthentication(Operation.values()[callbackId]);
            dialog.dismiss();
        });
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setSecondaryButtonListener(v -> {
            callbackInterface.cancelAuthentication();
            dialog.dismiss();
        });
        dialog.setOnCancelListener(v -> {
            callbackInterface.cancelAuthentication();
            dialog.dismiss();
        });
        dialog.setOnDismissListener(v-> {
            callbackInterface.cancelAuthentication();
        });
        dialog.show();
    }

    /**
     * Current behaviour: Don't allow users to create a private key unless the device is secure
     *
     * @param callbackId
     */
    private void showInsecure(int callbackId)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(context);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.device_insecure);
        dialog.setMessage(R.string.device_not_secure_warning);
        dialog.setButtonText(R.string.action_continue);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButtonListener(v -> {
            callbackInterface.cancelAuthentication();
            dialog.dismiss();
        });
        dialog.show();
    }
}
