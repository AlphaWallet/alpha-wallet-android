package io.stormbird.wallet.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.security.keystore.*;
import android.util.Log;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.AuthenticationCallback;
import io.stormbird.wallet.entity.ServiceErrorException;
import io.stormbird.wallet.widget.SignTransactionDialog;
import org.web3j.crypto.Hash;
import wallet.core.jni.CoinType;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;
import wallet.core.jni.PublicKey;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;

import static io.stormbird.wallet.entity.ServiceErrorException.*;

@TargetApi(23)
public class HDKeyService implements AuthenticationCallback
{
    private static final String TAG = "HDWallet";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String UNLOCK_KEY = "unlock_key";
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    private enum Operation
    {
        CREATE_HD_KEY, UNLOCK_HD_KEY
    }

    private static final int DEFAULT_KEY_STRENGTH = 128;
    private final Activity context;

    private HDWallet currentWallet;
    private String currentKey;
    private SignTransactionDialog signDialog;

    public HDKeyService(Activity ctx)
    {
        System.loadLibrary("TrustWalletCore");
        context = ctx;
    }

    //Create HDkey
    public String createNewHDKey()
    {
        currentWallet = new HDWallet(DEFAULT_KEY_STRENGTH, "key1");
        String mnemonic = currentWallet.mnemonic();
        checkAuthentication(Operation.CREATE_HD_KEY);
        PrivateKey pk = currentWallet.getKeyForCoin(CoinType.ETHEREUM);
        return CoinType.ETHEREUM.deriveAddress(pk);
    }

    private void createHDKey2()
    {
        storeHDWallet(currentWallet);
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
            System.out.println(mnemonic);
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

    private synchronized boolean storeHDWallet(HDWallet wallet)
    {
        KeyStore keyStore;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            PrivateKey pk = currentWallet.getKeyForCoin(CoinType.ETHEREUM);
            currentKey = CoinType.ETHEREUM.deriveAddress(pk);

            if (keyStore.containsAlias(currentKey)) { keyStore.deleteEntry(currentKey); } //TODO: Don't allow this

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
        signDialog.setBigText("Authenticate Credentials");
        signDialog.setSecondaryButtonText(R.string.action_cancel);
        signDialog.setPrimaryButtonText(R.string.dialog_title_sign_message);
        signDialog.setCanceledOnTouchOutside(true);
        signDialog.setOnCancelListener(v -> { authenticateFail("Cancelled"); });
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
            default:
                break;
        }
    }

    @Override
    public void authenticateFail(String fail)
    {
        signDialog.dismiss();
        //TODO: display fail dialog
    }
}
