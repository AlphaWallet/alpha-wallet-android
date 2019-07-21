package io.stormbird.wallet.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.security.keystore.*;
import android.util.Log;
import android.widget.Toast;
import io.reactivex.Single;
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
        CREATE_HD_KEY, FETCH_MNEMONIC, IMPORT_HD_KEY, SIGN_WITH_KEY, CHECK_AUTHENTICATION;
    }

    private static final int DEFAULT_KEY_STRENGTH = 128;
    private final Activity context;

    private String currentKey;
    private byte[] signData;
    private SignTransactionDialog signDialog;
    private CreateWalletCallbackInterface callbackInterface;
    private ImportWalletCallback importCallback;
    private SignAuthenticationCallback signCallback;

    private static Activity topmostActivity;

    public HDKeyService(Activity ctx)
    {
        System.loadLibrary("TrustWalletCore");
        if (ctx == null)
        {
            context = topmostActivity;
        }
        else
        {
            context = ctx;
        }
    }

    public void createNewHDKey(CreateWalletCallbackInterface callback)
    {
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        createHDKey();
    }

    public void getMnemonic(String address, CreateWalletCallbackInterface callback)
    {
        currentKey = address;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        unpackMnemonic(Operation.FETCH_MNEMONIC);
    }

    public void importHDKey(String seedPhrase, ImportWalletCallback callback)
    {
        importCallback = callback;
        //TODO: PIN Callback
        //cursory check for valid key import
        if (!HDWallet.isValid(seedPhrase))
        {
            callback.WalletValidated(null);
        }
        else
        {
            currentKey = seedPhrase;
            importHDKey();
        }
    }

    public void getAuthenticationForSignature(String walletAddr, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        currentKey = walletAddr;
        getAuthenticationForSignature();
    }

    private void getAuthenticationForSignature()
    {
        //check unlock status
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentKey, null);
            byte[] iv = readBytesFromFile(getFilePath(context, currentKey + "iv"));
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            signCallback.GotAuthorisation(true);
        }
        catch (UserNotAuthenticatedException e)
        {
            checkAuthentication(Operation.CHECK_AUTHENTICATION);
            return;
        }
        catch (Exception e)
        {
            //some other error, will exit the recursion with bad
            e.printStackTrace();
        }

        signCallback.GotAuthorisation(false);
    }

    public byte[] signData(String key, byte[] data)
    {
        signData = data;
        currentKey = key;
        //signData();
        return null;
    }

    private synchronized byte[] signData(String data, String keyAddress) throws ServiceErrorException
    {
        return null;
    }

    private synchronized void unpackMnemonic(Operation operation)
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
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

            switch (operation)
            {
                case FETCH_MNEMONIC:
                    callbackInterface.FetchMnemonic(mnemonic);
                    break;
                default:
                    break;
            }
        }
        catch (InvalidKeyException e)
        {
            if (e instanceof UserNotAuthenticatedException)
            {
                checkAuthentication(operation);
            }
            else
            {
                callbackInterface.tryAgain();
            }
        }
        catch (IOException | CertificateException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e)
        {
            callbackInterface.tryAgain();
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

    private void createHDKey()
    {
        HDWallet newWallet = new HDWallet(DEFAULT_KEY_STRENGTH, "key1");
        storeHDKey(newWallet, Operation.CREATE_HD_KEY);
    }

    private void importHDKey()
    {
        HDWallet newWallet = new HDWallet(currentKey, "key1");
        storeHDKey(newWallet, Operation.IMPORT_HD_KEY);
    }

    private synchronized void storeHDKey(HDWallet newWallet, Operation operation)
    {
        try
        {
            PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
            String address = CoinType.ETHEREUM.deriveAddress(pk);

            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            if (keyStore.containsAlias(address)) //re-import existing key.
            {
                deleteKey(keyStore, address);
            }

            KeyGenerator keyGenerator = getMaxSecurityKeyGenerator(address);

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            String path = getFilePath(context, address + "hd");
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = inCipher.getIV();
            String ivPath = getFilePath(context, address + "iv");
            boolean success = writeBytesToFile(ivPath, iv);
            if (!success) {
                keyStore.deleteEntry(address + "iv");
                failToStore(operation);
                throw new ServiceErrorException(
                        ServiceErrorException.FAIL_TO_SAVE_IV_FILE,
                        "Failed to saveTokens the iv file for: " + address + "iv");
            }

            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(path),
                    inCipher))
            {
                cipherOutputStream.write(newWallet.mnemonic().getBytes());
            }
            catch (Exception ex)
            {
                failToStore(operation);
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_STORE_ERROR,
                        "Failed to saveTokens the file for: " + address);
            }

            //blank class var
            currentKey = null;

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

            switch (operation)
            {
                case CREATE_HD_KEY:
                    callbackInterface.HDKeyCreated(address, context);
                    break;
                case IMPORT_HD_KEY:
                    importCallback.WalletValidated(currentKey);
                    break;
            }
            return;
        }
        catch (UserNotAuthenticatedException e)
        {
            //User isn't authenticated, get authentication and start again
            checkAuthentication(operation);
            return;
        }
        catch (Exception ex)
        {
            Log.d(TAG, "Key store error", ex);
        }

        failToStore(operation);
    }

    private void failToStore(Operation operation)
    {
        switch (operation)
        {
            case CREATE_HD_KEY:
                callbackInterface.HDKeyCreated(ZERO_ADDRESS, context);
                break;
            case IMPORT_HD_KEY:
                importCallback.WalletValidated(null);
                break;
        }
    }

    private KeyGenerator getMaxSecurityKeyGenerator(String keyAddress)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tryInitStrongBoxKey(keyGenerator, keyAddress))
            {
                Log.d(TAG, "Using Strongbox");
            }
            else
            {
                //fallback to non Strongbox
                tryInitUserAuthenticatedKey(keyGenerator, keyAddress);
            }
        }
        catch (InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
            keyGenerator = null;
        }

        return keyGenerator;
    }

    private boolean tryInitStrongBoxKey(KeyGenerator keyGenerator, String keyAddress) throws InvalidAlgorithmParameterException
    {
        try
        {
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    keyAddress,
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

    private void tryInitUserAuthenticatedKey(KeyGenerator keyGenerator, String keyAddress) throws InvalidAlgorithmParameterException
    {
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                keyAddress,
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
                createHDKey();
                break;
            case FETCH_MNEMONIC:
                unpackMnemonic(Operation.FETCH_MNEMONIC);
                break;
            case IMPORT_HD_KEY:
                importHDKey();
                break;
            case SIGN_WITH_KEY:
                //signData();
                break;
            case CHECK_AUTHENTICATION:
                getAuthenticationForSignature();
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

    public static void setTopmostActivity(Activity activity)
    {
        topmostActivity = activity;
    }
}
