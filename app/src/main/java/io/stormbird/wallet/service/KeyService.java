package io.stormbird.wallet.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.security.keystore.*;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.SignTransactionDialog;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import wallet.core.jni.PrivateKey;
import wallet.core.jni.*;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static io.stormbird.wallet.entity.WalletType.KEYSTORE_LEGACY;
import static io.stormbird.wallet.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static io.stormbird.wallet.service.KeyService.Operation.*;
import static io.stormbird.wallet.service.KeystoreAccountService.KEYSTORE_FOLDER;
import static io.stormbird.wallet.service.LegacyKeystore.getLegacyPassword;

@TargetApi(23)
public class KeyService implements AuthenticationCallback, PinAuthenticationCallbackInterface
{
    private static final String TAG = "HDWallet";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    public static final String HDKEY_LABEL = "hd";
    public static final String NO_AUTH_LABEL = "-noauth-";
    public static final String KEYSTORE_LABEL = "ks";

    //This value determines the time interval between the user swiping away the backup warning notice and it re-appearing
    public static final int TIME_BETWEEN_BACKUP_WARNING_MILLIS = 1000 * 60 * 60 * 24 * 30; //30 days //1000 * 60 * 3; //3 minutes for testing

    //Used mainly for re-entrant encrypt/decrypt operations for types of keys
    public enum Operation
    {
        CREATE_HD_KEY, FETCH_MNEMONIC, IMPORT_HD_KEY, SIGN_WITH_KEY, CHECK_AUTHENTICATION, SIGN_DATA,
        CREATE_NON_AUTHENTICATED_KEY, UPGRADE_HD_KEY, CREATE_KEYSTORE_KEY, UPGRADE_KEYSTORE_KEY,
        CREATE_PRIVATE_KEY, RESTORE_NON_AUTHENTICATED_HD_KEY, RESTORE_NON_AUTHENTICATED_KS_KEY
    }

    public enum AuthenticationLevel
    {
        NOT_SET, TEE_NO_AUTHENTICATION, TEE_AUTHENTICATION, STRONGBOX_NO_AUTHENTICATION, STRONGBOX_AUTHENTICATION
    }

    //Return values for requesting security upgrade of key
    public enum UpgradeKeyResult
    {
        REQUESTING_SECURITY, NO_SCREENLOCK, ALREADY_LOCKED, ERROR
    }

    //Check performed at service start to determine API strength
    private enum SecurityStatus
    {
        NOT_CHECKED, HAS_NO_TEE, HAS_TEE, HAS_STRONGBOX
    }

    private static final int DEFAULT_KEY_STRENGTH = 128;
    private final Context context;
    private Activity activity;

    //Used for keeping the Ethereum address between re-entrant calls
    private String currentKeyAddress;

    private AuthenticationLevel authLevel;
    private SignTransactionDialog signDialog;
    private AWalletAlertDialog alertDialog;
    private CreateWalletCallbackInterface callbackInterface;
    private ImportWalletCallback importCallback;
    private SignAuthenticationCallback signCallback;

    private static SecurityStatus securityStatus = SecurityStatus.NOT_CHECKED;

    public KeyService(Context ctx)
    {
        System.loadLibrary("TrustWalletCore");
        context = ctx;
        checkSecurity();
    }

    /**
     * Create a new HD key. Callback is not used if the key does not need to be created as locked.
     * @param callingActivity
     * @param callback
     */
    public void createNewHDKey(Activity callingActivity, CreateWalletCallbackInterface callback)
    {
        activity = callingActivity;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        createHDKey();
    }

    /**
     * Create and encrypt/store keystore password for importing a keystore. Flow for importing a private key is almost identical
     *
     * Possible flows are:
     *
     * 1. Create the random bytes that will be the keystore password (createPassword)
     * 2. Attempt to store the bytes used an auth-locked key (storeEncryptedBytes)
     *    - If authentication event is required go to (a.) otherwise flow forward to 3.
     *      a. Re-enter storeEncryptedBytes but store the password without requiring auth-lock
     *      b. Display fingerprint/PIN dialog
     *      c. On receiving authentication pass re-start flow at authenticatePass(int callbackId). If authentication fails e.
     *      d. authenticatePass switches flow for a return from CREATE_KEYSTORE_KEY and resumes flow back at 1.
     *      e. Authentication fails. Return import error. TODO: still create import key but don't authenticate lock.
     *  3. Store password using authentication-locked AES cipher.
     *  4. Return operation to calling Activity: either importCallback.KeystoreValidated for CREATE_KEYSTORE_KEY
     *      or importCallback.KeyValidated for CREATE_PRIVATE_KEY
     *
     * @param address
     * @param callingActivity
     * @param callback
     */
    public void createKeystorePassword(String address, Activity callingActivity, ImportWalletCallback callback)
    {
        activity = callingActivity;
        importCallback = callback;
        callback.setupAuthenticationCallback(this);
        currentKeyAddress = address;
        createPassword(CREATE_KEYSTORE_KEY);
    }

    /**
     * Flow is the same as createKeystorePassword but on successful completion of key generation call
     * importCallback.KeyValidated
     *
     * @param address
     * @param callingActivity
     * @param callback
     */
    public void createPrivateKeyPassword(String address, Activity callingActivity, ImportWalletCallback callback)
    {
        activity = callingActivity;
        importCallback = callback;
        callback.setupAuthenticationCallback(this);
        currentKeyAddress = address;
        createPassword(CREATE_PRIVATE_KEY);
    }

    /**
     * Encrypt and store mnemonic for HDWallet
     *
     * 1. Check valid seed phrase, generate HDWallet and attempt to store the key.
     * 2. storeHDKey calls storeEncryptedBytes which determines the key address and attempts to store the mnemonic bytes
     *    - If authentication event is required go to (a.) otherwise flow forward to 3.
     *      a. Re-enter storeEncryptedBytes but store the mnemonic without requiring auth-lock
     *      b. Display fingerprint/PIN dialog
     *      c. On receiving authentication pass re-start flow at authenticatePass(int callbackId). If authentication fails e.
     *      d. authenticatePass switches flow for a return from IMPORT_HD_KEY, reads the key stored at a. and passes it back into stage 2.
     *      e. Authentication fails. The non-locked key will be used, flow is passed back to calling activity.
     *  3. Mnemonic is stored with authentication-locked AES cipher.
     *  4. Pass flow back to calling activity via importCallback.WalletValidated
     *
     * @param seedPhrase
     * @param callingActivity
     * @param callback
     */
    public void importHDKey(String seedPhrase, Activity callingActivity, ImportWalletCallback callback)
    {
        activity = callingActivity;
        importCallback = callback;
        callback.setupAuthenticationCallback(this);

        //cursory check for valid key import
        if (!HDWallet.isValid(seedPhrase))
        {
            callback.WalletValidated(null, AuthenticationLevel.NOT_SET);
        }
        else
        {
            HDWallet newWallet = new HDWallet(seedPhrase, "");
            //now attempt to store a full authentication locked key
            //if auth is required, the storeEncryptedBytes function will store an encrypted, unlocked temporary version
            //then restore this version once we have a user-authentication event, then delete it once the locked key is created
            storeHDKey(newWallet, Operation.IMPORT_HD_KEY);
        }
    }

    /**
     * Fetch mnemonic from storage
     *
     * 1. call unpackMnemonic
     * 2. unpackMnemonic attempts to use the AES cipher in the KeyStore to decrypt the encrypted mnemonic password bytes
     *    - If authentication is required go to (a.) otherwise flow to 3.
     *      a. Display fingerprint/PIN dialog
     *      b. On receiving authentication event re-start flow at authenticatePass. Re-enter at 2. unpackMnemonic
     *      c. If authentication is cancelled or fails return flow via authenticateFail
     * 3. decrypt mnemonic and resume flow with callbackInterface.FetchMnemonic
     *
     * @param address
     * @param callingActivity
     * @param callback
     */
    public void getMnemonic(String address, Activity callingActivity, CreateWalletCallbackInterface callback)
    {
        activity = callingActivity;
        currentKeyAddress = address;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        unpackMnemonic(address, Operation.FETCH_MNEMONIC);
    }

    /**
     * Called before requesting a signature to fulfil an authentication event
     *
     * The operation of signing is slightly different from the other operations due to not wanting to change the structure
     * of the signing operation. There is no activity in the sign request code, so we should see if authentication is required
     * from the activity before starting a sign flow.
     *
     * Simply tries to restore the key for this wallet; if authentication is required then display authentication dialog and
     * resume flow as per the import functions.
     *
     * @param walletAddr
     * @param callingActivity
     * @param callback
     */
    public void getAuthenticationForSignature(String walletAddr, Activity callingActivity, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        callback.setupAuthenticationCallback(this);
        activity = callingActivity;
        currentKeyAddress = walletAddr;
        if (isChecking()) return; //guard against resetting existing dialog request

        Wallet wallet = new Wallet(walletAddr);
        wallet.checkWalletType(context);
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                signCallback.GotAuthorisation(true); //Legacy keys don't require authentication
                break;
            case KEYSTORE:
            case HDKEY:
                getAuthenticationForSignature();
                break;
            case NOT_DEFINED:
            case TEXT_MARKER:
            case WATCH:
                signCallback.GotAuthorisation(false);
                break;
        }
    }

    /**
     * Upgrade key security
     *
     * Works much the same as the import functions, but care must be taken not to destroy the existing key during upgrade
     * In the process of attempting to encrypt using an auth-locked key, we necessarily overwrite the existing key.
     * By attempting encryption we may trigger UserNotAuthenticatedException. In this case, the old key is already overwritten.
     * Therefore, the flow re-writes the old keystore password/mnemonic bytes using a non-authentication locked AES key.
     *
     * The flow asks for authentication, if provided it will restore the password/mnemonic bytes from the key generated stored in the last step
     * and again attempt to store the password/mnemonic bytes using an authentication-locked key. This time it will pass and the key is created.
     * If Authentication fails there is a usable fallback key available.
     *
     *
     * @param key
     * @param callingActivity
     * @param callback
     * @return
     */
    public UpgradeKeyResult upgradeKeySecurity(String key, Activity callingActivity, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        activity = callingActivity;
        callback.setupAuthenticationCallback(this);
        //first check we have ability to generate the key
        if (!deviceIsLocked())
            return UpgradeKeyResult.NO_SCREENLOCK;

        currentKeyAddress = key;

        return upgradeKey();
    }

    /**
     * SignData
     *
     * Flow for this function is by necessity simpler - this function is called from code that doesn't have access to an Activity, so can't create
     * any signing dialog. The authentication event must be generated prior to entering the signing flow.
     *
     * If HDWallet - decrypt mnemonic, regenerate private key, generate digest, sign digest using Trezor libs.
     * If Keystore - fetch keystore JSON file, decrypt keystore password, regenerate Web3j Credentials and sign.
     *
     * @param key
     * @param transactionBytes
     * @return
     */
    public synchronized byte[] signData(String key, byte[] transactionBytes)
    {
        currentKeyAddress = key;
        Wallet wallet = new Wallet(currentKeyAddress);
        wallet.checkWalletType(context);
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                return signWithKeystore(wallet, transactionBytes);
            case KEYSTORE:
                return signWithKeystore(wallet, transactionBytes);
            case HDKEY:
                String mnemonic = unpackMnemonic(currentKeyAddress, Operation.SIGN_DATA);
                if (mnemonic.length() == 0)
                    return "0000".getBytes();
                HDWallet newWallet = new HDWallet(mnemonic, "");
                PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
                byte[] digest = Hash.keccak256(transactionBytes);
                return pk.sign(digest, Curve.SECP256K1);
            case NOT_DEFINED:
            case TEXT_MARKER:
            case WATCH:
            default:
                return "0000".getBytes();
        }
    }


    /**
     * Fetches keystore password for export/backup of keystore
     *
     * @param address
     * @param callingActivity
     * @param callback
     */
    public void getPassword(String address, Activity callingActivity, CreateWalletCallbackInterface callback)
    {
        activity = callingActivity;
        currentKeyAddress = address;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        Wallet wallet = new Wallet(address);
        wallet.checkWalletType(context);

        String password = null;

        try
        {
            switch (wallet.type)
            {
                case KEYSTORE:
                    password = unpackMnemonic(wallet.address, Operation.FETCH_MNEMONIC);
                    break;
                case KEYSTORE_LEGACY:
                    password = new String(getLegacyPassword(context, wallet.address));
                    break;
                default:
                    break;
            }
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (password != null)
        {
            callback.FetchMnemonic(password);
        }
    }


    /*********************************
     * Internal Functions
     */

    private void getAuthenticationForSignature()
    {
        //check unlock status
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentKeyAddress, null);
            String encryptedHDKeyPath = getEncryptedFilePath(currentKeyAddress);//getFilePath(context, currentKey + "hd");
            boolean fileExists = new File(encryptedHDKeyPath).exists();
            if (!fileExists || secretKey == null)
            {
                signCallback.GotAuthorisation(false);
                return;
            }
            byte[] iv = readBytesFromFile(getFilePath(context, currentKeyAddress + "iv"));
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            final GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
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
            //see if we can automatically recover the key
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

    private synchronized String unpackMnemonic(String keyAddr, Operation operation)
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(keyAddr))
            {
                keyFailure("Key not found in keystore. Re-import key.");
                return "";
            }

            //create a stream to the encrypted bytes
            FileInputStream encryptedHDKeyBytes = getEncryptedFileStream(keyAddr);

            SecretKey secretKey = (SecretKey) keyStore.getKey(keyAddr, null);
            boolean ivExists = new File(getFilePath(context, keyAddr + "iv")).exists();
            byte[] iv = null;

            if (ivExists)
                iv = readBytesFromFile(getFilePath(context, keyAddr + "iv"));
            if (iv == null || iv.length == 0)
            {
                keyFailure("Cannot setup wallet seed.");
                return "";
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            final GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            CipherInputStream cipherInputStream = new CipherInputStream(encryptedHDKeyBytes, outCipher);
            byte[] mnemonicBytes = readBytesFromStream(cipherInputStream);
            String mnemonic = new String(mnemonicBytes);

            switch (operation)
            {
                case FETCH_MNEMONIC:
                    callbackInterface.FetchMnemonic(mnemonic);
                    break;
                case UPGRADE_HD_KEY:
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

    private FileInputStream getEncryptedFileStream(String currentKey) throws FileNotFoundException
    {
        String encryptedHDKeyPath = getEncryptedFilePath(currentKey);
        if (encryptedHDKeyPath != null) return new FileInputStream(encryptedHDKeyPath);
        else throw new FileNotFoundException("No key file found");
    }

    private void createHDKey()
    {
        HDWallet newWallet = new HDWallet(DEFAULT_KEY_STRENGTH, "");
        storeHDKey(newWallet, Operation.CREATE_NON_AUTHENTICATED_KEY); //create non-authenticated key initially
    }

    /**
     * Called after an authentication event was required, and the user has completed the authentication event
     */
    private void importHDKey()
    {
        //first recover the seed phrase from non-authlocked key. This removes the need to keep the seed phrase as a member on the heap
        // - making the key operation more secure
        String seedPhrase = unpackMnemonic(currentKeyAddress, UPGRADE_HD_KEY);
        HDWallet newWallet = new HDWallet(seedPhrase, "");
        storeHDKey(newWallet, Operation.IMPORT_HD_KEY);
    }

    private UpgradeKeyResult upgradeKey()
    {
        try
        {
            WalletType type = Wallet.getKeystoreType(context, currentKeyAddress);
            //now check it's an unlocked key
            String existingEncryptedKey = getEncryptedFilePath(currentKeyAddress);
            if (existingEncryptedKey != null && (existingEncryptedKey.contains(NO_AUTH_LABEL) || type == KEYSTORE_LEGACY))
            {
                Operation keyOperation;
                String secretData = null;

                switch (type)
                {
                    case KEYSTORE:
                        secretData = unpackMnemonic(currentKeyAddress, Operation.UPGRADE_HD_KEY);
                        keyOperation = Operation.UPGRADE_KEYSTORE_KEY;
                        break;
                    case KEYSTORE_LEGACY:
                        secretData = new String(getLegacyPassword(context, currentKeyAddress));
                        keyOperation = Operation.UPGRADE_KEYSTORE_KEY;
                        break;
                    case HDKEY:
                        secretData = unpackMnemonic(currentKeyAddress, Operation.UPGRADE_HD_KEY);
                        keyOperation = Operation.UPGRADE_HD_KEY;
                        break;
                    default:
                        return UpgradeKeyResult.ERROR;
                }

                if (secretData == null)
                    return UpgradeKeyResult.ERROR;

                storeEncryptedBytes(currentKeyAddress, secretData.getBytes(), keyOperation);

                return UpgradeKeyResult.REQUESTING_SECURITY;
            }
            else
            {
                return UpgradeKeyResult.ALREADY_LOCKED;
            }
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
            return UpgradeKeyResult.ERROR;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return UpgradeKeyResult.ERROR;
        }
    }

    private synchronized void storeHDKey(HDWallet newWallet, Operation operation)
    {
        PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
        currentKeyAddress = CoinType.ETHEREUM.deriveAddress(pk);

        storeEncryptedBytes(currentKeyAddress, newWallet.mnemonic().getBytes(), operation);
    }

    private synchronized void storeEncryptedBytes(String address, byte[] data, Operation operation)
    {
        KeyStore keyStore = null;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            boolean useAuth = keyIsAuthLocked(operation);
            String encryptedHDKeyPath = createEncryptedFilePath(address, operation);

            KeyGenerator keyGenerator = getMaxSecurityKeyGenerator(address, useAuth);
            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            String ivPath = getFilePath(context, address + "iv");
            boolean success = writeBytesToFile(ivPath, iv);
            if (!success)
            {
                deleteKey(keyStore, address);
                failToStore(operation);
                throw new ServiceErrorException(
                        ServiceErrorException.FAIL_TO_SAVE_IV_FILE,
                        "Failed to saveTokens the iv file for: " + address + "iv");
            }

            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedHDKeyPath),
                    cipher))
            {
                cipherOutputStream.write(data);
            }
            catch (Exception ex)
            {
                deleteKey(keyStore, address);
                failToStore(operation);
                throw new ServiceErrorException(
                        ServiceErrorException.KEY_STORE_ERROR,
                        "Failed to saveTokens the file for: " + address);
            }

            switch (operation)
            {
                case CREATE_HD_KEY:
                case CREATE_NON_AUTHENTICATED_KEY:
                    if (callbackInterface != null)
                        callbackInterface.HDKeyCreated(address, context, authLevel);
                    break;
                case IMPORT_HD_KEY:
                    importCallback.WalletValidated(address, authLevel);
                    deleteNonAuthKeyEncryptedKeyBytes(address); //in the case the user re-imported a key, destroy the backup key
                    break;
                case UPGRADE_HD_KEY:
                case UPGRADE_KEYSTORE_KEY:
                    signCallback.CreatedKey(address);
                    deleteNonAuthKeyEncryptedKeyBytes(address);
                    break;
                case CREATE_KEYSTORE_KEY:
                    importCallback.KeystoreValidated(new String(data), authLevel);
                    break;
                case CREATE_PRIVATE_KEY:
                    importCallback.KeyValidated(new String(data), authLevel);
                    break;
                default:
                    break;
            }

            return;
        }
        catch (UserNotAuthenticatedException e)
        {
            //Store non-authenticated key if required.
            //This keeps the operation modular
            switch (operation)
            {
                case IMPORT_HD_KEY:
                case UPGRADE_HD_KEY:
                    storeEncryptedBytes(address, data, RESTORE_NON_AUTHENTICATED_HD_KEY);
                    break;
                case UPGRADE_KEYSTORE_KEY:
                    storeEncryptedBytes(address, data, RESTORE_NON_AUTHENTICATED_KS_KEY);
                    break;
                default:
                    deleteKey(keyStore, address);
                    break;
            }

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

    private KeyGenerator getMaxSecurityKeyGenerator(String keyAddress, boolean useAuthentication)
    {
        KeyGenerator keyGenerator;

        try
        {
            keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE);
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException ex)
        {
            ex.printStackTrace();
            return null;
        }

        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tryInitStrongBoxKey(keyGenerator, keyAddress, useAuthentication))
            {
                Log.d(TAG, "Using Strongbox");
                if (useAuthentication) authLevel = AuthenticationLevel.STRONGBOX_AUTHENTICATION;
                else authLevel = AuthenticationLevel.STRONGBOX_NO_AUTHENTICATION;
            }
            else if (tryInitTEEKey(keyGenerator, keyAddress, useAuthentication))
            {
                //fallback to non Strongbox
                Log.d(TAG, "Using Hardware security TEE");
                if (useAuthentication) authLevel = AuthenticationLevel.TEE_AUTHENTICATION;
                else authLevel = AuthenticationLevel.TEE_NO_AUTHENTICATION;
            }
            else if (tryInitTEEKey(keyGenerator, keyAddress, false))
            {
                Log.d(TAG, "Using Hardware security TEE without authentication");
                authLevel = AuthenticationLevel.TEE_NO_AUTHENTICATION;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //handle unable to generate key - should be impossible to get here after API 19
            authLevel = AuthenticationLevel.NOT_SET;
        }

        return keyGenerator;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private boolean tryInitStrongBoxKey(KeyGenerator keyGenerator, String keyAddress, boolean useAuthentication) throws InvalidAlgorithmParameterException
    {
        try
        {
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    keyAddress,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                      .setBlockModes(BLOCK_MODE)
                                      .setKeySize(256)
                                      .setUserAuthenticationRequired(useAuthentication)
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

    private boolean tryInitTEEKey(KeyGenerator keyGenerator, String keyAddress, boolean useAuthentication)
    {
        try
        {
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    keyAddress,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                      .setBlockModes(BLOCK_MODE)
                                      .setKeySize(256)
                                      .setUserAuthenticationRequired(useAuthentication)
                                      .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                                      .setRandomizedEncryptionRequired(true)
                                      .setEncryptionPaddings(PADDING)
                                      .build());
        }
        catch (IllegalStateException | InvalidAlgorithmParameterException e)
        {
            //couldn't create the key because of no lock
            return false;
        }

        return true;
    }

    private void checkAuthentication(Operation operation)
    {
        signDialog = new SignTransactionDialog(activity, operation.ordinal());
        signDialog.setCanceledOnTouchOutside(false);
        signDialog.setCancelListener(v -> {
            authenticateFail("Cancelled", AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, operation.ordinal());
        });
        signDialog.show();
        signDialog.getFingerprintAuthorisation(this);
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
        if (signDialog != null && signDialog.isShowing())
            signDialog.dismiss();
        //resume key operation
        Operation operation = Operation.values()[callbackId];
        switch (operation)
        {
            case CREATE_HD_KEY:
                createHDKey();
                break;
            case FETCH_MNEMONIC:
                unpackMnemonic(currentKeyAddress, Operation.FETCH_MNEMONIC);
                break;
            case IMPORT_HD_KEY:
                importHDKey();
                break;
            case CHECK_AUTHENTICATION:
                getAuthenticationForSignature();
                break;
            case UPGRADE_HD_KEY:
            case UPGRADE_KEYSTORE_KEY:
                upgradeKey();
                break;
            case CREATE_KEYSTORE_KEY:
            case CREATE_PRIVATE_KEY:
                createPassword(operation);
                break;
            default:
                break;
        }
    }

    @Override
    public void authenticateFail(String fail, AuthenticationFailType failType, int callbackId)
    {
        System.out.println("AUTH FAIL: " + failType.ordinal());

        switch (failType)
        {
            case AUTHENTICATION_DIALOG_CANCELLED:
                cancelAuthentication();
                if (signDialog != null && signDialog.isShowing())
                    signDialog.dismiss();
                break;
            case FINGERPRINT_NOT_VALIDATED:
                vibrate();
                Toast.makeText(context, "Fingerprint authentication failed", Toast.LENGTH_SHORT).show();
                break;
            case PIN_FAILED:
                vibrate();
                break;
            case DEVICE_NOT_SECURE:
                //Note:- allowing user to create a key with no auth-unlock ensures we should never get here
                //Handle some sort of edge condition where the user gets here.
                showInsecure(callbackId);
                break;
        }

        if (callbackId == Operation.UPGRADE_HD_KEY.ordinal())
        {
            signCallback.GotAuthorisation(false);
        }

        if (activity == null || activity.isDestroyed())
        {
            cancelAuthentication();
        }
    }

    private void keyFailure(String message)
    {
        if (message == null || message.length() == 0 || !AuthorisationFailMessage(message))
        {
            if (callbackInterface != null)
                callbackInterface.keyFailure(message);
            else if (signCallback != null)
                signCallback.GotAuthorisation(false);
        }
    }

    private void cancelAuthentication()
    {
        if (callbackInterface != null)
            callbackInterface.cancelAuthentication();
        else if (signCallback != null)
            signCallback.GotAuthorisation(false);
    }

    public boolean isChecking()
    {
        return (signDialog != null && signDialog.isShowing());
    }

    private boolean AuthorisationFailMessage(String message)
    {
        if (alertDialog != null && alertDialog.isShowing())
            alertDialog.dismiss();
        if (activity == null || activity.isDestroyed())
            return false;

        alertDialog = new AWalletAlertDialog(activity);
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
        AWalletAlertDialog dialog = new AWalletAlertDialog(activity);
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

    private void createPassword(Operation operation)
    {
        //generate password
        byte[] newPassword = new byte[256];
        SecureRandom random;
        try
        {
            //attempt to use superior source of randomness
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                random = SecureRandom.getInstanceStrong(); //this can throw a NoSuchAlgorithmException
            }
            else
            {
                random = new SecureRandom();
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            random = new SecureRandom();
        }

        random.nextBytes(newPassword);

        //attempt to store this password. NB this may result in a callback firing off then re-entering this function.
        storeEncryptedBytes(currentKeyAddress, newPassword, operation);  //because we'll now only ever be importing keystore, always create with Auth
    }

    private synchronized byte[] signWithKeystore(Wallet wallet, byte[] transactionBytes)
    {
        //1. get password from store
        //2. construct credentials
        //3. sign
        byte[] sigBytes = "0000".getBytes();

        try
        {
            String password = "";
            switch (wallet.type)
            {
                case KEYSTORE:
                    password = unpackMnemonic(wallet.address, Operation.SIGN_DATA);
                    break;
                case KEYSTORE_LEGACY:
                    password = new String(getLegacyPassword(context, wallet.address));
                    break;
                default:
                    break;
            }

            File keyFolder = new File(context.getFilesDir(), KEYSTORE_FOLDER);
            Credentials credentials = KeystoreAccountService.getCredentials(keyFolder, wallet.address, password);
            Sign.SignatureData signatureData = Sign.signMessage(
                    transactionBytes, credentials.getEcKeyPair());
            sigBytes = KeystoreAccountService.bytesFromSignature(signatureData);
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return sigBytes;
    }


    /**
     * Return a list of HD Key wallets in date order, from first created
     *
     * @return List of Wallet, date ordered
     */
    public List<Wallet> getAllHDWallets()
    {
        List<Wallet> wallets = new ArrayList<>();
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            Enumeration<String> keys = keyStore.aliases();
            List<Date> fileDates = new ArrayList<>();
            Map<Date, Wallet> walletMap = new HashMap<>();

            while (keys.hasMoreElements())
            {
                String alias = keys.nextElement();
                String encryptedHDFileName = getEncryptedFilePath(alias);
                if (encryptedHDFileName != null && encryptedHDFileName.contains(HDKEY_LABEL))
                {
                    File hdEncryptedBytes = new File(encryptedHDFileName);
                    Date date = new Date(hdEncryptedBytes.lastModified());
                    fileDates.add(date);
                    if (!alias.startsWith("0x"))
                        alias = "0x" + alias;
                    Wallet hdKey = new Wallet(alias);
                    hdKey.type = WalletType.HDKEY;

                    switch (securityStatus)
                    {
                        default:
                            if (encryptedHDFileName.contains(NO_AUTH_LABEL)) hdKey.authLevel = AuthenticationLevel.TEE_NO_AUTHENTICATION;
                            else hdKey.authLevel = AuthenticationLevel.TEE_AUTHENTICATION;
                            break;
                        case HAS_STRONGBOX:
                            if (encryptedHDFileName.contains(NO_AUTH_LABEL)) hdKey.authLevel = AuthenticationLevel.STRONGBOX_NO_AUTHENTICATION;
                            else hdKey.authLevel = AuthenticationLevel.STRONGBOX_AUTHENTICATION;
                            break;
                    }

                    walletMap.put(date, hdKey);
                }
            }

            Collections.sort(fileDates);

            for (Date d : fileDates)
            {
                wallets.add(walletMap.get(d));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return wallets;
    }

    private String createEncryptedFilePath(String address, Operation operation)
    {
        String fileName = address;
        switch (operation)
        {
            case UPGRADE_HD_KEY:
            case IMPORT_HD_KEY:
            case CREATE_HD_KEY:
                fileName += HDKEY_LABEL;
                break;

            case RESTORE_NON_AUTHENTICATED_HD_KEY:
            case CREATE_NON_AUTHENTICATED_KEY:
                fileName = fileName + NO_AUTH_LABEL + HDKEY_LABEL;
                break;
            case CREATE_KEYSTORE_KEY:
            case CREATE_PRIVATE_KEY:
                if (deviceIsLocked())
                {
                    fileName += KEYSTORE_LABEL;
                }
                else
                {
                    fileName = fileName + NO_AUTH_LABEL + KEYSTORE_LABEL;
                }
                break;
            case RESTORE_NON_AUTHENTICATED_KS_KEY:
                fileName = fileName + NO_AUTH_LABEL + KEYSTORE_LABEL;
                break;
            case UPGRADE_KEYSTORE_KEY:
                fileName += KEYSTORE_LABEL;
                break;
            default:
                break;
        }

        return getFilePath(context, fileName);
    }

    private boolean keyIsAuthLocked(Operation operation)
    {
        switch (operation)
        {
            case UPGRADE_HD_KEY:
            case CREATE_HD_KEY:
            case UPGRADE_KEYSTORE_KEY:
            case IMPORT_HD_KEY:
                return true;
            case CREATE_NON_AUTHENTICATED_KEY:
            case RESTORE_NON_AUTHENTICATED_HD_KEY:
            case RESTORE_NON_AUTHENTICATED_KS_KEY:
                return false;
            case CREATE_KEYSTORE_KEY:
            case CREATE_PRIVATE_KEY:
                return deviceIsLocked();
            default:
                return true;
        }
    }

    private String getEncryptedFilePath(String keyAddr)
    {
        String authEncryptedHDKeyPath = getFilePath(context, keyAddr + HDKEY_LABEL);
        String noAuthEncryptedHDKeyPath = getFilePath(context, keyAddr + NO_AUTH_LABEL + HDKEY_LABEL);
        String authEncryptedKeystorePath = getFilePath(context, keyAddr + KEYSTORE_LABEL);
        String noAuthEncryptedKeystorePath = getFilePath(context, keyAddr + NO_AUTH_LABEL + KEYSTORE_LABEL);
        String legacyKeyBytes = getFilePath(context, keyAddr);
        if (new File(authEncryptedHDKeyPath).exists()) return authEncryptedHDKeyPath;
        else if (new File(noAuthEncryptedHDKeyPath).exists()) return noAuthEncryptedHDKeyPath;
        else if (new File(authEncryptedKeystorePath).exists()) return authEncryptedKeystorePath;
        else if (new File(noAuthEncryptedKeystorePath).exists()) return noAuthEncryptedKeystorePath;
        else if (new File(legacyKeyBytes).exists()) return legacyKeyBytes;
        else return null;
    }


    /*
            Utility methods
     */

    static byte[] readBytesFromFile(String path)
    {
        byte[] bytes = null;
        FileInputStream fin;
        try
        {
            File file = new File(path);
            fin = new FileInputStream(file);
            bytes = readBytesFromStream(fin);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return bytes;
    }


    public synchronized static String getFilePath(Context context, String fileName)
    {
        return new File(context.getFilesDir(), fileName).getAbsolutePath();
    }

    private boolean writeBytesToFile(String path, byte[] data)
    {
        FileOutputStream fos = null;
        try
        {
            File file = new File(path);
            fos = new FileOutputStream(file);
            // Writes bytes from the specified byte array to this file output stream
            fos.write(data);
            return true;
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File not found" + e);
        }
        catch (IOException ioe)
        {
            System.out.println("Exception while writing file " + ioe);
        }
        finally
        {
            // close the streams using close method
            try
            {
                if (fos != null)
                {
                    fos.close();
                }
            }
            catch (IOException ioe)
            {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
        return false;
    }

    static byte[] readBytesFromStream(InputStream in)
    {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        try
        {
            while ((len = in.read(buffer)) != -1)
            {
                byteBuffer.write(buffer, 0, len);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                byteBuffer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }


    private synchronized void deleteKey(KeyStore keyStore, String keyAddr)
    {
        try
        {
            String encryptedBytesPath = getEncryptedFilePath(keyAddr);
            if (encryptedBytesPath != null && new File(encryptedBytesPath).exists())  new File(encryptedBytesPath).delete();
            File iv = new File(getFilePath(context, keyAddr + "iv"));
            if (iv.exists())
                iv.delete();
            if (keyStore != null && keyStore.containsAlias(keyAddr))
                keyStore.deleteEntry(keyAddr);
        }
        catch (KeyStoreException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Delete a non-auth key
     * note that the iv and keystore key will have been replaced by the newly minted auth key
     *
     * @param address
     */
    private void deleteNonAuthKeyEncryptedKeyBytes(String address)
    {
        File nonAuthKey = new File(getFilePath(context, address + NO_AUTH_LABEL + HDKEY_LABEL));
        File nonAuthKeystore = new File(getFilePath(context, address + NO_AUTH_LABEL + KEYSTORE_LABEL));
        File legacyKey = new File(getFilePath(context, address));
        if (nonAuthKey.exists()) nonAuthKey.delete();
        if (nonAuthKeystore.exists()) nonAuthKeystore.delete();
        if (legacyKey.exists()) legacyKey.delete();
    }

    /**
     * Delete all traces of the key in Android keystore, encrypted bytes and iv file in private data area
     * @param keyAddress
     */
    public synchronized void deleteKeyCompletely(String keyAddress)
    {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(keyAddress);
            String encryptedPasswordBytes = getEncryptedFilePath(keyAddress);
            if (encryptedPasswordBytes == null)
            {
                //try legacy keystore
                if (new File(getFilePath(context, keyAddress)).exists()) encryptedPasswordBytes = getFilePath(context, keyAddress);
            }
            if (encryptedPasswordBytes != null)
            {
                new File(getFilePath(context, encryptedPasswordBytes)).delete();
                new File(getFilePath(context, keyAddress + "iv")).delete();
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private void checkSecurity()
    {
        if (securityStatus == SecurityStatus.NOT_CHECKED)
        {
            getMaxSecurityKeyGenerator(ZERO_ADDRESS, false);
            switch (authLevel)
            {
                case NOT_SET:
                    securityStatus = SecurityStatus.HAS_NO_TEE;
                    break;
                case TEE_NO_AUTHENTICATION:
                case TEE_AUTHENTICATION:
                    securityStatus = SecurityStatus.HAS_TEE;
                    break;
                case STRONGBOX_NO_AUTHENTICATION:
                case STRONGBOX_AUTHENTICATION:
                    securityStatus = SecurityStatus.HAS_STRONGBOX;
                    break;
            }
        }
    }

    public static boolean hasStrongbox()
    {
        return securityStatus == SecurityStatus.HAS_STRONGBOX;
    }

    private boolean deviceIsLocked()
    {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null) return false;
        else return keyguardManager.isDeviceSecure();
    }

    public void checkWalletType(Wallet wallet)
    {
        wallet.checkWalletType(context);
    }

    private void vibrate()
    {
        Vibrator vb = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null && vb.hasVibrator())
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect vibe = VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE);
                vb.vibrate(vibe);
            }
            else
            {
                //noinspection deprecation
                vb.vibrate(200);
            }
        }
    }
}
