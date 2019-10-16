package com.alphawallet.app.service;

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
import com.alphawallet.app.BuildConfig;

import com.alphawallet.app.R;

import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.AuthenticationFailType;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.KeyServiceException;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.ServiceErrorException;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import wallet.core.jni.PrivateKey;
import wallet.core.jni.*;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static com.alphawallet.app.entity.Operation.*;
import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.service.KeystoreAccountService.KEYSTORE_FOLDER;
import static com.alphawallet.app.service.LegacyKeystore.getLegacyPassword;

@TargetApi(23)
public class KeyService implements AuthenticationCallback, PinAuthenticationCallbackInterface
{
    private static final String TAG = "HDWallet";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;
    public  static final String FAILED_SIGNATURE = "00000000000000000000000000000000000000000000000000000000000000000";

    //This value determines the time interval between the user swiping away the backup warning notice and it re-appearing
    public static final int TIME_BETWEEN_BACKUP_WARNING_MILLIS = 1000 * 60 * 60 * 24 * 30; //30 days //1000 * 60 * 3; //3 minutes for testing

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

    //Used for keeping the Ethereum account information between re-entrant calls
    private Wallet currentWallet;

    private AuthenticationLevel authLevel;
    private SignTransactionDialog signDialog;
    private AWalletAlertDialog alertDialog;
    private CreateWalletCallbackInterface callbackInterface;
    private ImportWalletCallback importCallback;
    private SignAuthenticationCallback signCallback;

    private static SecurityStatus securityStatus = SecurityStatus.NOT_CHECKED;

    public Context getContext()
    {
        return context;
    }

    public KeyService(Context ctx)
    {
        System.loadLibrary("TrustWalletCore");
        context = ctx;
        checkSecurity();
    }

    /**
     * Create a new HD key.
     * call createHDKey which creates a new HD wallet, stores the mnemonic and calls HDKeyCreated callback
     * We use a callback to allow creation of a key with authentication lock
     *
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
     * Create and encrypt/store an authentication-locked keystore password for importing a keystore.
     * Flow for importing a private key is almost identical
     *
     * Flow is as follows:
     *
     * 1. Obtain authentication event - pop up the unlock dialog.
     * 2. After authentication event, proceed to authenticatePass and switch through to createPassword()
     * 3. Create a new strong keystore password, store the password.
     * 4. Call the KeystoreValidated callback.
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
        currentWallet = new Wallet(address);
        checkAuthentication(CREATE_KEYSTORE_KEY);
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
        currentWallet = new Wallet(address);
        checkAuthentication(CREATE_PRIVATE_KEY);
    }

    /**
     * Encrypt and store mnemonic for HDWallet
     *
     * 1. Check valid seed phrase, generate HDWallet and store the mnemonic without authentication lock
     * 2. Obtain authentication event.
     * 3. After authentication pass through to authenticatePass and switch to importHDKey()
     * 4. ImportHDKey() restores the mnemonic and replaces the key with an authentication locked key.
     * 5. KeyValidated callback to pass control back to viewModel
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
            storeHDKey(newWallet, false); //store encrypted bytes in case of re-entry
            checkAuthentication(IMPORT_HD_KEY);
        }
    }

    /**
     * Fetch mnemonic from storage
     *
     * 1. call unpackMnemonic
     * 2. if authentication required, get authentication event and call unpackMnemonic
     * 3. return mnemonic to FetchMnemonic callback
     *
     * @param wallet
     * @param callingActivity
     * @param callback
     */
    public void getMnemonic(Wallet wallet, Activity callingActivity, CreateWalletCallbackInterface callback)
    {
        activity = callingActivity;
        currentWallet = wallet;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);

        try
        {
            String mnemonic = unpackMnemonic();
            callback.FetchMnemonic(mnemonic);
        }
        catch (KeyServiceException e)
        {
            keyFailure(e.getMessage());
        }
        catch (UserNotAuthenticatedException e)
        {
            checkAuthentication(FETCH_MNEMONIC);
        }
    }

    /**
     * 1. Get authentication event if required.
     * 2. Resume operation at getAuthenticationForSignature
     * 3. get mnemonic/password
     * 4. rebuild private key
     * 5. sign.
     *
     * @param wallet
     * @param callingActivity
     * @param callback
     */
    public void getAuthenticationForSignature(Wallet wallet, Activity callingActivity, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        callback.setupAuthenticationCallback(this);
        activity = callingActivity;
        currentWallet = wallet;
        if (isChecking()) return; //guard against resetting existing dialog request

        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                signCallback.GotAuthorisation(true); //Legacy keys don't require authentication
                break;
            case KEYSTORE:
            case HDKEY:
                checkAuthentication(Operation.CHECK_AUTHENTICATION);
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
     * 1. Get authentication, and then execute 'upgradeKey()' from authenticatePass
     * 2. Upgrade key reads the mnemonic/password, then calls storeEncryptedBytes with authentication.
     * 3. returns result and flow back to callee via signCallback.CreatedKey
     *
     *
     * @param wallet
     * @param callingActivity
     * @param callback
     * @return
     */
    public UpgradeKeyResult upgradeKeySecurity(Wallet wallet, Activity callingActivity, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        activity = callingActivity;
        currentWallet = wallet;
        callback.setupAuthenticationCallback(this);
        //first check we have ability to generate the key
        if (!deviceIsLocked())
            return UpgradeKeyResult.NO_SCREENLOCK;

        //request authentication
        switch (wallet.type)
        {
            case KEYSTORE:
            case KEYSTORE_LEGACY:
                checkAuthentication(UPGRADE_KEYSTORE_KEY);
                return UpgradeKeyResult.REQUESTING_SECURITY;
            case HDKEY:
                checkAuthentication(UPGRADE_HD_KEY);
                return UpgradeKeyResult.REQUESTING_SECURITY;
            default:
                break;
        }

        return UpgradeKeyResult.ERROR;
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
     * @param wallet
     * @param transactionBytes
     * @return
     */
    public synchronized byte[] signData(Wallet wallet, byte[] transactionBytes)
    {
        currentWallet = wallet;
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                return signWithKeystore(transactionBytes);
            case KEYSTORE:
                return signWithKeystore(transactionBytes);
            case HDKEY:
                try
                {
                    String mnemonic = unpackMnemonic();
                    HDWallet newWallet = new HDWallet(mnemonic, "");
                    PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
                    byte[] digest = Hash.keccak256(transactionBytes);
                    return pk.sign(digest, Curve.SECP256K1);
                }
                catch (KeyServiceException e)
                {
                    keyFailure(e.getMessage());
                }
                catch (UserNotAuthenticatedException e)
                {
                    checkAuthentication(FETCH_MNEMONIC);
                }
                return FAILED_SIGNATURE.getBytes();
            case NOT_DEFINED:
            case TEXT_MARKER:
            case WATCH:
            default:
                keyFailure(context.getString(R.string.no_key));
                return FAILED_SIGNATURE.getBytes();
        }
    }

    /**
     * Fetches keystore password for export/backup of keystore
     *
     * @param wallet
     * @param callingActivity
     * @param callback
     */
    public void getPassword(Wallet wallet, Activity callingActivity, CreateWalletCallbackInterface callback)
    {
        activity = callingActivity;
        currentWallet = wallet;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);

        String password;

        try
        {
            switch (wallet.type)
            {
                case KEYSTORE:
                    password = unpackMnemonic();
                    callback.FetchMnemonic(password);
                    break;
                case KEYSTORE_LEGACY:
                    password = new String(getLegacyPassword(context, wallet.address));
                    callback.FetchMnemonic(password);
                    break;
                default:
                    break;
            }
        }
        catch (UserNotAuthenticatedException e)
        {
            checkAuthentication(FETCH_MNEMONIC);
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
        }
        catch (KeyServiceException e)
        {
            keyFailure(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentWallet.address, null);
            String encryptedHDKeyPath = getFilePath(context, currentWallet.address);
            if (!new File(encryptedHDKeyPath).exists() || secretKey == null)
            {
                signCallback.GotAuthorisation(false);
                return;
            }
            byte[] iv = readBytesFromFile(getFilePath(context, currentWallet.address + "iv"));
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

    private synchronized String unpackMnemonic() throws KeyServiceException, UserNotAuthenticatedException
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(currentWallet.address))
            {
                throw new KeyServiceException("Key not found in keystore. Re-import key.");
            }

            //create a stream to the encrypted bytes
            FileInputStream encryptedHDKeyBytes = new FileInputStream(getFilePath(context, currentWallet.address));
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentWallet.address, null);
            boolean ivExists = new File(getFilePath(context, currentWallet.address + "iv")).exists();
            byte[] iv = null;

            if (ivExists)
                iv = readBytesFromFile(getFilePath(context, currentWallet.address + "iv"));
            if (iv == null || iv.length == 0)
            {
                throw new KeyServiceException("Cannot setup wallet seed.");
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            final GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            CipherInputStream cipherInputStream = new CipherInputStream(encryptedHDKeyBytes, outCipher);
            byte[] mnemonicBytes = readBytesFromStream(cipherInputStream);
            return new String(mnemonicBytes);
        }
        catch (InvalidKeyException e)
        {
            if (e instanceof UserNotAuthenticatedException)
            {
                throw new UserNotAuthenticatedException("Requires Authentication");
            }
            else
            {
                throw new KeyServiceException(e.getMessage());
            }
        }
        catch (UnrecoverableKeyException e)
        {
            throw new KeyServiceException("Key created at different security level. Please re-import key");
        }
        catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
            throw new KeyServiceException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new KeyServiceException(e.getMessage());
        }
    }

    private void createHDKey()
    {
        HDWallet newWallet = new HDWallet(DEFAULT_KEY_STRENGTH, "");
        boolean success = storeHDKey(newWallet, false); //create non-authenticated key initially
        if (callbackInterface != null)
            callbackInterface.HDKeyCreated(success ? currentWallet.address : null, context, authLevel);
    }

    /**
     * Called after an authentication event was required, and the user has completed the authentication event
     */
    private void importHDKey()
    {
        //first recover the seed phrase from non-authlocked key. This removes the need to keep the seed phrase as a member on the heap
        // - making the key operation more secure
        try
        {
            String seedPhrase = unpackMnemonic();
            HDWallet newWallet = new HDWallet(seedPhrase, "");
            boolean success = storeHDKey(newWallet, true);
            String reportAddress = success ? currentWallet.address : null;
            importCallback.WalletValidated(reportAddress, authLevel);
        }
        catch (UserNotAuthenticatedException e)
        {
            //Should not get this. Authentication has already been requested and key should not be auth-locked at this stage
            checkAuthentication(IMPORT_HD_KEY);
        }
        catch (KeyServiceException e)
        {
            keyFailure(e.getMessage());
        }
    }

    /**
     * Reached after authentication has been provided
     * @return
     */
    private void upgradeKey()
    {
        try
        {
            String secretData = null;

            switch (currentWallet.type)
            {
                case HDKEY:
                case KEYSTORE:
                    secretData = unpackMnemonic();
                    break;
                case KEYSTORE_LEGACY:
                    secretData = new String(getLegacyPassword(context, currentWallet.address));
                    break;
                default:
                    break;
            }

            if (secretData == null) return;

            boolean keyStored = storeEncryptedBytes(secretData.getBytes(), true);
            if (keyStored)
            {
                signCallback.CreatedKey(currentWallet.address);
            }
            else
            {
                signCallback.CreatedKey(ZERO_ADDRESS);
            }
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
            signCallback.CreatedKey(ZERO_ADDRESS);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            signCallback.CreatedKey(ZERO_ADDRESS);
        }
    }

    private synchronized boolean storeHDKey(HDWallet newWallet, boolean keyRequiresAuthentication)
    {
        PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
        currentWallet = new Wallet(CoinType.ETHEREUM.deriveAddress(pk));

        return storeEncryptedBytes(newWallet.mnemonic().getBytes(), keyRequiresAuthentication);
    }

    private synchronized boolean storeEncryptedBytes(byte[] data, boolean createAuthLocked)
    {
        KeyStore keyStore = null;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            String encryptedHDKeyPath = getFilePath(context, currentWallet.address);
            KeyGenerator keyGenerator = getMaxSecurityKeyGenerator(currentWallet.address, createAuthLocked);
            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            String ivPath = getFilePath(context, currentWallet.address + "iv");
            boolean success = writeBytesToFile(ivPath, iv);
            if (!success)
            {
                deleteKey(currentWallet.address);
                throw new ServiceErrorException(
                        ServiceErrorException.ServiceErrorCode.FAIL_TO_SAVE_IV_FILE,
                        "Failed to saveTokens the iv file for: " + currentWallet.address + "iv");
            }

            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedHDKeyPath),
                    cipher))
            {
                cipherOutputStream.write(data);
            }
            catch (Exception ex)
            {
                deleteKey(currentWallet.address);
                throw new ServiceErrorException(
                        ServiceErrorException.ServiceErrorCode.KEY_STORE_ERROR,
                        "Failed to saveTokens the file for: " + currentWallet.address);
            }

            return true;
        }
        catch (Exception ex)
        {
            deleteKey(currentWallet.address);
            Log.d(TAG, "Key store error", ex);
        }

        return false;
    }

    private KeyGenerator getMaxSecurityKeyGenerator(String keyAddress, boolean useAuthentication)
    {
        KeyGenerator keyGenerator = null;

        try
        {
            keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tryInitStrongBoxKey(keyGenerator, keyAddress, useAuthentication))
            {
                Log.d(TAG, "Using Strongbox");
                if (useAuthentication) authLevel = AuthenticationLevel.STRONGBOX_AUTHENTICATION;
                else authLevel = AuthenticationLevel.STRONGBOX_NO_AUTHENTICATION;
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tryInitStrongBoxKey(keyGenerator, keyAddress, false))
            {
                Log.d(TAG, "Using Strongbox");
                authLevel = AuthenticationLevel.STRONGBOX_NO_AUTHENTICATION;
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
        catch (NoSuchAlgorithmException | NoSuchProviderException ex)
        {
            ex.printStackTrace();
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
        catch (InvalidAlgorithmParameterException e)
        {
            Log.d(TAG, "Strongbox with no auth");
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
        //first check if the phone is unlocked
        String dialogTitle;
        switch (operation)
        {
            case IMPORT_HD_KEY:
            case CREATE_HD_KEY:
            case UPGRADE_HD_KEY:
            case CREATE_KEYSTORE_KEY:
            case UPGRADE_KEYSTORE_KEY:
            case CREATE_PRIVATE_KEY:
                dialogTitle = context.getString(R.string.provide_authentication);
                break;
            case FETCH_MNEMONIC:
            case CHECK_AUTHENTICATION:
            case SIGN_DATA:
            default:
                dialogTitle = context.getString(R.string.unlock_private_key);
                break;
        }

        //see if unlock is required
        if (!requiresUnlock() && signCallback != null)
        {
            signCallback.GotAuthorisation(true);
            return;
        }

        signDialog = new SignTransactionDialog(activity, operation, dialogTitle, null);
        signDialog.setCanceledOnTouchOutside(false);
        signDialog.setCancelListener(v -> {
            authenticateFail("Cancelled", AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, operation);
        });
        signDialog.setOnDismissListener(v -> {
            signDialog = null;
        });
        signDialog.show();
        signDialog.getFingerprintAuthorisation(this);
    }


    @Override
    public void CompleteAuthentication(Operation callbackId)
    {
        authenticatePass(callbackId);
    }

    @Override
    public void FailedAuthentication(Operation taskCode)
    {
        authenticateFail("Authentication fail", AuthenticationFailType.PIN_FAILED, taskCode);
    }

    @Override
    public void authenticatePass(Operation operation)
    {
        if (signDialog != null && signDialog.isShowing())
            signDialog.dismiss();
        //resume key operation
        switch (operation)
        {
            case CREATE_HD_KEY: //Note: not currently used: may be used if we create an HD key with authentication
                createHDKey();
                break;
            case FETCH_MNEMONIC:
                try
                {
                    callbackInterface.FetchMnemonic(unpackMnemonic());
                }
                catch (UserNotAuthenticatedException e)
                {
                    checkAuthentication(FETCH_MNEMONIC);
                }
                catch (KeyServiceException e)
                {
                    keyFailure(e.getMessage());
                }
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
    public void authenticateFail(String fail, AuthenticationFailType failType, Operation callbackId)
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

        if (callbackId == UPGRADE_HD_KEY)
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
    private void showInsecure(Operation callbackId)
    {
        //only show the 'not secure' message on certain occasions. Otherwise just pass through.
        switch (callbackId)
        {
            case CREATE_HD_KEY:
            case IMPORT_HD_KEY:
            case CREATE_PRIVATE_KEY:
            case CREATE_KEYSTORE_KEY:
            case UPGRADE_KEYSTORE_KEY:
            case UPGRADE_HD_KEY:
                //warn user their phone is insecure
                break;
            default:
                //proceed to use key, don't show unlocked warning
                authenticatePass(callbackId);
                return;
        }

        AWalletAlertDialog dialog = new AWalletAlertDialog(activity);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.device_insecure);
        dialog.setMessage(R.string.device_not_secure_warning);
        dialog.setButtonText(R.string.action_continue);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButtonListener(v -> {
            //proceed with operation
            switch (callbackId)
            {
                case UPGRADE_KEYSTORE_KEY:
                case UPGRADE_HD_KEY:
                    //dismiss sign dialog & cancel authentication
                    if (signDialog != null && signDialog.isShowing())
                        signDialog.dismiss();
                    cancelAuthentication();
                    break;
                default:
                    authenticatePass(callbackId);
                    break;
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    /**
     * Only ever called after authentication event
     */
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

        boolean success = storeEncryptedBytes(newPassword, true);  //because we'll now only ever be importing keystore, always create with Auth if possible

        if (!success)
        {
            AuthorisationFailMessage(context.getString(R.string.please_enable_security));
        }
        else
        {
            switch (operation)
            {
                case CREATE_KEYSTORE_KEY:
                    importCallback.KeystoreValidated(new String(newPassword), authLevel);
                    break;
                case CREATE_PRIVATE_KEY:
                    importCallback.KeyValidated(new String(newPassword), authLevel);
                    break;
            }
        }
    }

    private synchronized byte[] signWithKeystore(byte[] transactionBytes)
    {
        //1. get password from store
        //2. construct credentials
        //3. sign
        byte[] sigBytes = FAILED_SIGNATURE.getBytes();

        try
        {
            String password = "";
            switch (currentWallet.type)
            {
                case KEYSTORE:
                    password = unpackMnemonic();
                    break;
                case KEYSTORE_LEGACY:
                    password = new String(getLegacyPassword(context, currentWallet.address));
                    break;
                default:
                    break;
            }

            File keyFolder = new File(context.getFilesDir(), KEYSTORE_FOLDER);
            Credentials credentials = KeystoreAccountService.getCredentials(keyFolder, currentWallet.address, password);
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

    /**
     * Delete all traces of the key in Android keystore, encrypted bytes and iv file in private data area
     * @param keyAddress
     */
    public synchronized void deleteKey(String keyAddress)
    {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias(keyAddress)) keyStore.deleteEntry(keyAddress);
            File encryptedKeyBytes = new File(getFilePath(context, keyAddress));
            File encryptedBytesFileIV = new File(getFilePath(context, keyAddress + "iv"));
            if (encryptedKeyBytes.exists()) encryptedKeyBytes.delete();
            if (encryptedBytesFileIV.exists()) encryptedBytesFileIV.delete();
        } catch (Exception e) {
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

    private boolean requiresUnlock()
    {
        try
        {
            unpackMnemonic();
        }
        catch (UserNotAuthenticatedException e)
        {
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public void resetSigningDialog()
    {
        if (signDialog != null && signDialog.isShowing())
        {
            signDialog.dismiss();
        }
        signDialog = null;
    }

    private boolean deviceIsLocked()
    {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null) return false;
        else return keyguardManager.isDeviceSecure();
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
