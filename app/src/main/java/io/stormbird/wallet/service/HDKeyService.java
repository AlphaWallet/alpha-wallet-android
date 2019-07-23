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
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SignTransactionDialog;
import wallet.core.jni.*;
import wallet.core.jni.PrivateKey;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
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

    public static final int TIME_BETWEEN_BACKUP_MILLIS = 1000*60*1; //TODO: RESTORE 30 DAYS. TESTING: 1 minute  //1000 * 60 * 60 * 24 * 30; //30 days
    public static final int TIME_BETWEEN_BACKUP_WARNING_MILLIS = 1000*60*1; //TODO: RESTORE 30 DAYS. TESTING: 1 minute  //1000 * 60 * 60 * 24 * 30; //30 days

    public enum Operation
    {
        CREATE_HD_KEY, FETCH_MNEMONIC, IMPORT_HD_KEY, SIGN_WITH_KEY, CHECK_AUTHENTICATION, SIGN_DATA
    }

    public enum AuthenticationLevel
    {
        NOT_SET, NO_AUTHENTICATION, HARDWARE_AUTHENTICATION, STRONGBOX_AUTHENTICATION
    }

    private static final int DEFAULT_KEY_STRENGTH = 128;
    private final Activity context;

    private AuthenticationLevel authLevel;
    private String currentKey;
    private SignTransactionDialog signDialog;
    private AWalletAlertDialog alertDialog;
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
            topmostActivity = ctx;
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
        callback.setupAuthenticationCallback(this);

        //cursory check for valid key import
        if (!HDWallet.isValid(seedPhrase))
        {
            callback.WalletValidated(null, AuthenticationLevel.NOT_SET);
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
        callback.setupAuthenticationCallback(this);
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
            String encryptedHDKeyPath = getFilePath(context, currentKey + "hd");
            boolean fileExists = new File(encryptedHDKeyPath).exists();
            if (!fileExists || secretKey == null)
            {
                signCallback.GotAuthorisation(false);
                return;
            }
            byte[] iv = readBytesFromFile(getFilePath(context, currentKey + "iv"));
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            signCallback.GotAuthorisation(true);
            return;
        }
        catch (UserNotAuthenticatedException e)
        {
            checkAuthentication(Operation.CHECK_AUTHENTICATION);
            return;
        }
        catch (KeyPermanentlyInvalidatedException | UnrecoverableKeyException e)
        {
            keyFailure("Key created at different security level. Please re-import key");
            e.printStackTrace();
        }
        catch (Exception e)
        {
            //some other error, will exit the recursion with bad
            e.printStackTrace();
        }

        signCallback.GotAuthorisation(false);
    }

    //Auth must be unlocked
    synchronized byte[] signData(String key, byte[] transactionBytes)
    {
        currentKey = key;
        String mnemonic = unpackMnemonic(Operation.SIGN_DATA);
        if (mnemonic.length() == 0) return new String("0000").getBytes();
        HDWallet newWallet = new HDWallet(mnemonic, "key1");
        PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
        byte[] digest = Hash.keccak256(transactionBytes);
        byte[] signature = pk.sign(digest, Curve.SECP256K1);
        return signature;
    }

    private synchronized String unpackMnemonic(Operation operation)
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(currentKey)) { keyFailure("Key not found in keystore. Re-import key."); return ""; }
            String encryptedHDKeyPath = getFilePath(context, currentKey + "hd");
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentKey, null);
            boolean ivExists = new File(getFilePath(context, currentKey + "iv")).exists();
            byte[] iv =  null;

            if (ivExists) iv = readBytesFromFile(getFilePath(context, currentKey + "iv"));
            if (iv == null || iv.length == 0)
            {
                keyFailure("Cannot setup wallet seed.");
                return "";
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedHDKeyPath), outCipher);
            byte[] mnemonicBytes = readBytesFromStream(cipherInputStream);
            String mnemonic = new String(mnemonicBytes);

            switch (operation)
            {
                case FETCH_MNEMONIC:
                    callbackInterface.FetchMnemonic(mnemonic);
                    break;
                case SIGN_DATA:
                    return mnemonic;
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
                keyFailure(e.getMessage());
            }
        }
        catch (UnrecoverableKeyException e)
        {
            keyFailure("Key created at different security level. Please re-import key");
            e.printStackTrace();
        }
        catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
            keyFailure(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return "";
    }

    private synchronized void deleteKey(KeyStore keyStore, String keyAddr) {
        try {
            File encrypted = new File(getFilePath(context, keyAddr + "hd"));
            File iv = new File(getFilePath(context, keyAddr + "iv"));
            if (encrypted.exists()) encrypted.delete();
            if (iv.exists()) iv.delete();
            if (keyStore != null && keyStore.containsAlias(keyAddr)) keyStore.deleteEntry(keyAddr);
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
        String address = "";
        KeyStore keyStore = null;
        try
        {
            PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
            address = CoinType.ETHEREUM.deriveAddress(pk);

            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            Enumeration<String> keys = keyStore.aliases();

            while (keys.hasMoreElements())
            {
                System.out.println("Key: " + keys.nextElement());
            }

            if (keyStore.containsAlias(address)) //re-import existing key - no harm done as address is generated from mnemonic
            {
                deleteKey(keyStore, address);
            }

            KeyGenerator keyGenerator = getMaxSecurityKeyGenerator(address);

            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            String encryptedHDKeyPath = getFilePath(context, address + "hd");
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
                    new FileOutputStream(encryptedHDKeyPath),
                    inCipher))
            {
                cipherOutputStream.write(newWallet.mnemonic().getBytes());
            }
            catch (Exception ex)
            {
                deleteKey(keyStore, address);
                failToStore(operation);
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_STORE_ERROR,
                        "Failed to saveTokens the file for: " + address);
            }

            //blank class var
            currentKey = null;

            switch (operation)
            {
                case CREATE_HD_KEY:
                    callbackInterface.HDKeyCreated(address, context, authLevel);
                    break;
                case IMPORT_HD_KEY:
                    importCallback.WalletValidated(address, authLevel);
                    break;
            }
            return;
        }
        catch (UserNotAuthenticatedException e)
        {
            //delete keys if created
            deleteKey(keyStore, address);
            //User isn't authenticated, get authentication and start again
            checkAuthentication(operation);
            return;
        }
        catch (Exception ex)
        {
            deleteKey(keyStore, address);
            Log.d(TAG, "Key store error", ex);
        }

        failToStore(operation);
    }

    private void failToStore(Operation operation)
    {
        switch (operation)
        {
            case CREATE_HD_KEY:
                callbackInterface.HDKeyCreated(ZERO_ADDRESS, context, AuthenticationLevel.NOT_SET);
                break;
            case IMPORT_HD_KEY:
                importCallback.WalletValidated(null, AuthenticationLevel.NOT_SET);
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
                authLevel = AuthenticationLevel.STRONGBOX_AUTHENTICATION;
            }
            else
            {
                tryInitUserAuthenticatedKey(keyGenerator, keyAddress);
                //fallback to non Strongbox
                Log.d(TAG, "Using Hardware security TEE");
                authLevel = AuthenticationLevel.HARDWARE_AUTHENTICATION;
            }
        }
        catch (InvalidAlgorithmParameterException e)
        {
            if (tryInitZeroAuthenticationKey(keyGenerator, keyAddress))
            {
                authLevel = AuthenticationLevel.NO_AUTHENTICATION;
            }
            else
            {
                e.printStackTrace();
                keyGenerator = null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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

    private boolean tryInitUserAuthenticatedKey(KeyGenerator keyGenerator, String keyAddress) throws InvalidAlgorithmParameterException
    {
        try
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
        catch (IllegalStateException|InvalidAlgorithmParameterException e)
        {
            //couldn't create the key because of no lock
            return false;
        }

        return true;
    }

    private boolean tryInitZeroAuthenticationKey(KeyGenerator keyGenerator, String keyAddress)
    {
        try
        {
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    keyAddress,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                      .setBlockModes(BLOCK_MODE)
                                      .setKeySize(256)
                                      .setUserAuthenticationRequired(false)
                                      .setRandomizedEncryptionRequired(true)
                                      .setEncryptionPaddings(PADDING)
                                      .build());
        }
        catch (InvalidAlgorithmParameterException e)
        {
            return false;
        }

        return true;
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
        if (signDialog != null && signDialog.isShowing()) signDialog.dismiss();
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
                cancelAuthentication();
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
                break;
            case DEVICE_NOT_SECURE:
                //Note:- allowing user to create a key with no auth-unlock ensures we should never get here
                //Handle some sort of edge condition where the user gets here.
                showInsecure(callbackId);
                break;
        }

        if (context == null || context.isDestroyed())
        {
            cancelAuthentication();
        }
    }

    private void keyFailure(String message)
    {
        if (message == null || message.length() == 0 || !AuthorisationFailMessage(message))
        {
            if (callbackInterface != null) callbackInterface.keyFailure(message);
            else if (signCallback != null) signCallback.GotAuthorisation(false);
        }
    }

    private void cancelAuthentication()
    {
        if (callbackInterface != null) callbackInterface.cancelAuthentication();
        else if (signCallback != null) signCallback.GotAuthorisation(false);
    }

    public boolean isChecking()
    {
        return (signDialog != null && signDialog.isShowing());
    }

    private boolean AuthorisationFailMessage(String message)
    {
        if (alertDialog != null && alertDialog.isShowing()) alertDialog.dismiss();
        if (context == null || context.isDestroyed()) return false;

        alertDialog = new AWalletAlertDialog(context);
        alertDialog.setIcon(AWalletAlertDialog.ERROR);
        alertDialog.setTitle(R.string.key_error);
        alertDialog.setMessage(message);
        alertDialog.setButtonText(R.string.action_continue);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setButtonListener(v -> {
            keyFailure("");
            alertDialog.dismiss();
        });
        alertDialog.setOnCancelListener(v -> {
            keyFailure("");
            cancelAuthentication();
        });
        alertDialog.show();

        return true;
    }

    /**
     * Current behaviour: Allow user to create unsecured key
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
            cancelAuthentication();
            dialog.dismiss();
        });
        dialog.show();
    }

    public static void setTopmostActivity(Activity activity)
    {
        topmostActivity = activity;
    }
}
