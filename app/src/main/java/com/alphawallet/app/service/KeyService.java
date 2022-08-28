package com.alphawallet.app.service;

import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;
import static com.alphawallet.app.entity.Operation.CREATE_KEYSTORE_KEY;
import static com.alphawallet.app.entity.Operation.CREATE_PRIVATE_KEY;
import static com.alphawallet.app.entity.Operation.FETCH_MNEMONIC;
import static com.alphawallet.app.entity.Operation.IMPORT_HD_KEY;
import static com.alphawallet.app.entity.Operation.UPGRADE_HD_KEY;
import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.service.KeyService.AuthenticationLevel.STRONGBOX_NO_AUTHENTICATION;
import static com.alphawallet.app.service.KeyService.AuthenticationLevel.TEE_NO_AUTHENTICATION;
import static com.alphawallet.app.service.KeystoreAccountService.KEYSTORE_FOLDER;
import static com.alphawallet.app.service.LegacyKeystore.getLegacyPassword;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.AuthenticationCallback;
import com.alphawallet.app.entity.AuthenticationFailType;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.ServiceErrorException;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.KeyEncodingType;
import com.alphawallet.app.entity.cryptokeys.KeyServiceException;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.cryptokeys.SignatureReturnType;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import timber.log.Timber;
import wallet.core.jni.CoinType;
import wallet.core.jni.Curve;
import wallet.core.jni.HDWallet;
import wallet.core.jni.Hash;
import wallet.core.jni.PrivateKey;

@TargetApi(23)
public class KeyService implements AuthenticationCallback, PinAuthenticationCallbackInterface
{
    private static final String TAG = "HDWallet";
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;
    public  static final String FAILED_SIGNATURE = "00000000000000000000000000000000000000000000000000000000000000000";
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;

    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    public static final String LEGACY_CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    public static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    //This value determines the time interval between the user swiping away the backup warning notice and it re-appearing
    public static final long TIME_BETWEEN_BACKUP_WARNING_MILLIS = 1000L * 60L * 60L * 24L * 30L; //30 days //1000 * 60 * 3; //3 minutes for testing

    public enum AuthenticationLevel
    {
        NOT_SET, TEE_NO_AUTHENTICATION, TEE_AUTHENTICATION, STRONGBOX_NO_AUTHENTICATION, STRONGBOX_AUTHENTICATION
    }

    //Return values for requesting security upgrade of key
    public enum UpgradeKeyResultType
    {
        REQUESTING_SECURITY, NO_SCREENLOCK, ALREADY_LOCKED, ERROR, SUCCESSFULLY_UPGRADED
    }

    public class UpgradeKeyResult
    {
        public final UpgradeKeyResultType result;
        public final String message;

        public UpgradeKeyResult(UpgradeKeyResultType res, String msg)
        {
            result = res;
            message = msg;
        }
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
    private final AnalyticsServiceType<AnalyticsProperties> analyticsService;
    private boolean requireAuthentication = false;

    private static SecurityStatus securityStatus = SecurityStatus.NOT_CHECKED;

    public Context getContext()
    {
        return context;
    }

    public KeyService(Context ctx, AnalyticsServiceType<AnalyticsProperties> analyticsService)
    {
        System.loadLibrary("TrustWalletCore");
        this.context = ctx;
        this.analyticsService = analyticsService;
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

        //cursory check for valid key import
        if (!HDWallet.isValid(seedPhrase))
        {
            callback.walletValidated(null, KeyEncodingType.SEED_PHRASE_KEY, AuthenticationLevel.NOT_SET);
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

        //unlock key

        try
        {
            String mnemonic = unpackMnemonic();
            callback.fetchMnemonic(mnemonic);
        }
        catch (KeyServiceException e)
        {
            keyFailure(e.getMessage());
        }
        catch (UserNotAuthenticatedException e)
        {
            callingActivity.runOnUiThread(() ->
                    checkAuthentication(FETCH_MNEMONIC));
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
        activity = callingActivity;
        currentWallet = wallet;

        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                signCallback.gotAuthorisation(true); //Legacy keys don't require authentication
                break;
            case KEYSTORE:
            case HDKEY:
                checkAuthentication(Operation.CHECK_AUTHENTICATION);
                break;
            case NOT_DEFINED:
            case TEXT_MARKER:
            case WATCH:
                signCallback.gotAuthorisation(false);
                break;
        }
    }

    public void setRequireAuthentication()
    {
        requireAuthentication = true;
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
     * @return
     */
    public UpgradeKeyResult upgradeKeySecurity(Wallet wallet, Activity callingActivity)
    {
        signCallback = null;
        activity = callingActivity;
        currentWallet = wallet;
        //first check we have ability to generate the key
        if (!deviceIsLocked())
        {
            return new UpgradeKeyResult(UpgradeKeyResultType.NO_SCREENLOCK, "Device is not locked");
        }

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
     * @param wallet
     * @param TBSdata
     * @return
     */
    synchronized SignatureFromKey signData(Wallet wallet, byte[] TBSdata)
    {
        SignatureFromKey returnSig = new SignatureFromKey();
        returnSig.sigType = SignatureReturnType.KEY_AUTHENTICATION_ERROR;
        returnSig.signature = FAILED_SIGNATURE.getBytes();

        currentWallet = wallet;
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
            case KEYSTORE:
                returnSig = signWithKeystore(TBSdata);
                break;

            case HDKEY:
                try
                {
                    String mnemonic = unpackMnemonic();
                    HDWallet newWallet = new HDWallet(mnemonic, "");
                    PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
                    byte[] digest = Hash.keccak256(TBSdata);
                    returnSig.signature = pk.sign(digest, Curve.SECP256K1);
                    returnSig.sigType = SignatureReturnType.SIGNATURE_GENERATED;
                }
                catch (KeyServiceException | UserNotAuthenticatedException e)
                {
                    returnSig.failMessage = e.getMessage();
                }
                break;
            case WATCH:
                returnSig.failMessage = context.getString(R.string.action_watch_account);
                break;
            case NOT_DEFINED:
            case TEXT_MARKER:
            default:
                returnSig.failMessage = context.getString(R.string.no_key);
        }

        return returnSig;
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

        String password;

        try
        {
            switch (wallet.type)
            {
                case KEYSTORE:
                    password = unpackMnemonic();
                    callback.fetchMnemonic(password);
                    break;
                case KEYSTORE_LEGACY:
                    password = new String(getLegacyPassword(context, wallet.address));
                    callback.fetchMnemonic(password);
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
            if (!BuildConfig.DEBUG) analyticsService.recordException(e);
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

    public void resetSigningDialog()
    {
        if (signDialog != null) signDialog.close();
        signDialog = null;
    }

    private synchronized String unpackMnemonic() throws KeyServiceException, UserNotAuthenticatedException
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            String matchingAddr = findMatchingAddrInKeyStore(currentWallet.address);
            if (!keyStore.containsAlias(matchingAddr))
            {
                throw new KeyServiceException("Key not found in keystore. Re-import key.");
            }

            //create a stream to the encrypted bytes
            FileInputStream encryptedHDKeyBytes = new FileInputStream(getFilePath(context, matchingAddr));
            SecretKey secretKey = (SecretKey) keyStore.getKey(matchingAddr, null);
            boolean ivExists = new File(getFilePath(context, matchingAddr + "iv")).exists();
            byte[] iv = null;

            if (ivExists)
                iv = readBytesFromFile(getFilePath(context, matchingAddr + "iv"));
            if (iv == null || iv.length == 0)
            {
                throw new KeyServiceException(context.getString(R.string.cannot_read_encrypt_file));
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
                throw new UserNotAuthenticatedException(context.getString(R.string.authentication_error));
            }
            else
            {
                throw new KeyServiceException(e.getMessage());
            }
        }
        catch (UnrecoverableKeyException e)
        {
            throw new KeyServiceException(context.getString(R.string.device_security_changed));
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

    public Pair<KeyExceptionType, String> testCipher(String walletAddress, String cipherAlgorithm)
    {
        KeyExceptionType retVal = KeyExceptionType.UNKNOWN;
        String keyData = null;
        try
        {
            String encryptedDataFilePath = KeyService.getFilePath(context, walletAddress);
            String keyIv = KeyService.getFilePath(context, walletAddress + "iv");
            boolean ivExists = new File(keyIv).exists();
            boolean aliasExists = new File(encryptedDataFilePath).exists();

            if (!ivExists)
            {
                retVal = KeyExceptionType.IV_NOT_FOUND;
                throw new Exception("iv file doesn't exist");
            }
            if (!aliasExists)
            {
                retVal = KeyExceptionType.ENCRYPTED_FILE_NOT_FOUND;
                throw new Exception("Key file doesn't exist");
            }

            //test legacy key
            byte[] iv = KeyService.readBytesFromFile(keyIv);

            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(walletAddress, null);

            Cipher outCipher = Cipher.getInstance(cipherAlgorithm);
            final AlgorithmParameterSpec spec = cipherAlgorithm.equals(CIPHER_ALGORITHM) ? new GCMParameterSpec(128, iv) : new IvParameterSpec(iv);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] keyBytes = KeyService.readBytesFromStream(cipherInputStream);
            keyData = new String(keyBytes);
            retVal = KeyExceptionType.SUCCESSFUL_DECODE;
        }
        catch (UserNotAuthenticatedException e)
        {
            retVal = KeyExceptionType.REQUIRES_AUTH;
        }
        catch (InvalidKeyException e)
        {
            //Wrong spec
            retVal = KeyExceptionType.INVALID_CIPHER;
        }
        catch (Exception e)
        {
            // Other
        }

        return new Pair<>(retVal, keyData);
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
            importCallback.walletValidated(reportAddress, KeyEncodingType.SEED_PHRASE_KEY, authLevel);
        }
        catch (UserNotAuthenticatedException | KeyServiceException e)
        {
            keyFailure(e.getMessage());
        }
    }

    /**
     * Reached after authentication has been provided
     * @return
     */
    private UpgradeKeyResult upgradeKey()
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

            if (secretData == null)
            {
                return new UpgradeKeyResult(UpgradeKeyResultType.ERROR, context.getString(R.string.no_key_found));
            }

            boolean keyStored = storeEncryptedBytes(secretData.getBytes(), true, currentWallet.address);
            if (keyStored)
            {
                return new UpgradeKeyResult(UpgradeKeyResultType.SUCCESSFULLY_UPGRADED, "");
            }
            else
            {
                return new UpgradeKeyResult(UpgradeKeyResultType.ERROR, context.getString(R.string.unable_store_key, currentWallet.address));
            }
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) analyticsService.recordException(e);
            e.printStackTrace();
            return new UpgradeKeyResult(UpgradeKeyResultType.ERROR, e.getLocalizedMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new UpgradeKeyResult(UpgradeKeyResultType.ERROR, e.getLocalizedMessage());
        }
    }

    private synchronized boolean storeHDKey(HDWallet newWallet, boolean keyRequiresAuthentication)
    {
        PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
        currentWallet = new Wallet(CoinType.ETHEREUM.deriveAddress(pk));

        return storeEncryptedBytes(newWallet.mnemonic().getBytes(), keyRequiresAuthentication, currentWallet.address);
    }

    private synchronized boolean storeEncryptedBytes(byte[] data, boolean createAuthLocked, String fileName)
    {
        KeyStore keyStore = null;
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            String encryptedHDKeyPath = getFilePath(context, fileName);
            KeyGenerator keyGenerator = getMaxSecurityKeyGenerator(fileName, createAuthLocked);
            final SecretKey secretKey = keyGenerator.generateKey();
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            String ivPath = getFilePath(context, fileName + "iv");
            boolean success = writeBytesToFile(ivPath, iv);
            if (!success)
            {
                //deleteKey(fileName);
                throw new ServiceErrorException(
                        ServiceErrorException.ServiceErrorCode.FAIL_TO_SAVE_IV_FILE,
                        "Failed to create the iv file for: " + fileName + "iv");
            }

            try (CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedHDKeyPath),
                    cipher))
            {
                cipherOutputStream.write(data);
            }
            catch (Exception ex)
            {
                //deleteKey(fileName);
                throw new ServiceErrorException(
                        ServiceErrorException.ServiceErrorCode.KEY_STORE_ERROR,
                        "Failed to create the file for: " + fileName);
            }

            return true;
        }
        catch (Exception ex)
        {
            deleteKey(fileName);
            Timber.tag(TAG).d(ex, "Key store error");
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
                if (useAuthentication) authLevel = AuthenticationLevel.STRONGBOX_AUTHENTICATION;
                else authLevel = STRONGBOX_NO_AUTHENTICATION;
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tryInitStrongBoxKey(keyGenerator, keyAddress, false))
            {
                authLevel = STRONGBOX_NO_AUTHENTICATION;
            }
            else if (tryInitTEEKey(keyGenerator, keyAddress, useAuthentication))
            {
                //fallback to non Strongbox
                if (useAuthentication) authLevel = AuthenticationLevel.TEE_AUTHENTICATION;
                else authLevel = TEE_NO_AUTHENTICATION;
            }
            else if (tryInitTEEKey(keyGenerator, keyAddress, false))
            {
                authLevel = TEE_NO_AUTHENTICATION;
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
                                      .setInvalidatedByBiometricEnrollment(false)
                                      .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                                      .setRandomizedEncryptionRequired(true)
                                      .setEncryptionPaddings(PADDING)
                                      .build());

            keyGenerator.generateKey();
        }
        catch (StrongBoxUnavailableException e)
        {
            return false;
        }
        catch (InvalidAlgorithmParameterException e)
        {
            return false;
        }

        return true;
    }

    private boolean tryInitTEEKey(KeyGenerator keyGenerator, String keyAddress, boolean useAuthentication)
    {
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                keyGenerator.init(new KeyGenParameterSpec.Builder(
                        keyAddress,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(BLOCK_MODE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(useAuthentication)
                        .setInvalidatedByBiometricEnrollment(false)
                        .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                        .setRandomizedEncryptionRequired(true)
                        .setEncryptionPaddings(PADDING)
                        .build());
            }
            else
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
            case UPGRADE_HD_KEY:
            case UPGRADE_KEYSTORE_KEY:
            case CREATE_PRIVATE_KEY:
            case CREATE_KEYSTORE_KEY:
            case IMPORT_HD_KEY:
            case CREATE_HD_KEY:
                //always unlock for these conditions
                dialogTitle = context.getString(R.string.provide_authentication);
                break;
            case FETCH_MNEMONIC:
            case CHECK_AUTHENTICATION:
            case SIGN_DATA:
            default:
                dialogTitle = context.getString(R.string.unlock_private_key);
                //unlock may be optional here
                if (!requireAuthentication && (currentWallet.authLevel == TEE_NO_AUTHENTICATION || currentWallet.authLevel == STRONGBOX_NO_AUTHENTICATION)
                        && !requiresUnlock() && signCallback != null)
                {
                    signCallback.gotAuthorisation(true);
                    return;
                }
                break;
        }

        resetSigningDialog();

        signDialog = new SignTransactionDialog(context);
        signDialog.getAuthentication(this, activity, operation);
        requireAuthentication = false;
    }

    @Override
    public void completeAuthentication(Operation callbackId)
    {
        authenticatePass(callbackId);
    }

    @Override
    public void failedAuthentication(Operation taskCode)
    {
        authenticateFail("Authentication fail", AuthenticationFailType.PIN_FAILED, taskCode);
    }

    @Override
    public void authenticatePass(Operation operation)
    {
        //resume key operation
        switch (operation)
        {
            case CREATE_HD_KEY: //Note: not currently used: may be used if we create an HD key with authentication
                createHDKey();
                break;
            case FETCH_MNEMONIC:
                try
                {
                    callbackInterface.fetchMnemonic(unpackMnemonic());
                }
                catch (Exception e)
                {
                    keyFailure(e.getMessage());
                }
                break;
            case IMPORT_HD_KEY:
                importHDKey();
                break;
            case CHECK_AUTHENTICATION:
                signCallback.gotAuthorisation(true);
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
        switch (failType)
        {
            case AUTHENTICATION_DIALOG_CANCELLED: //user dialog cancel
                cancelAuthentication();
                break;
            case FINGERPRINT_ERROR_CANCELED:
                //called when user cancels the dialog
                return;
            case FINGERPRINT_NOT_VALIDATED:
                vibrate();
                Toast.makeText(context, R.string.fingerprint_authentication_failed, Toast.LENGTH_SHORT).show();
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
            signCallback.gotAuthorisation(false);
        }

        if (activity == null || activity.isDestroyed())
        {
            cancelAuthentication();
        }
    }

    @Override
    public void legacyAuthRequired(Operation callbackId, String dialogTitle, String desc)
    {
//        signDialog = new SignTransactionDialog2(activity, callbackId, dialogTitle, desc);
//        signDialog.setCanceledOnTouchOutside(false);
//        signDialog.setCancelListener(v -> {
//            authenticateFail("Cancelled", AuthenticationFailType.AUTHENTICATION_DIALOG_CANCELLED, callbackId);
//        });
//        signDialog.setOnDismissListener(v -> {
//            signDialog = null;
//        });
//        signDialog.show();
//        signDialog.getLegacyAuthentication(this);
//        requireAuthentication = false;
    }

    private void keyFailure(String message)
    {
        if (message == null || message.length() == 0 || !AuthorisationFailMessage(message))
        {
            if (callbackInterface != null)
                callbackInterface.keyFailure(message);
            else if (signCallback != null)
                signCallback.gotAuthorisation(false);
            else
                AuthorisationFailMessage(message);
        }
    }

    private void cancelAuthentication()
    {
        if (signCallback != null)
            signCallback.cancelAuthentication();
        else if (callbackInterface != null)
            callbackInterface.cancelAuthentication();
    }

    private boolean AuthorisationFailMessage(String message)
    {
        if (alertDialog != null && alertDialog.isShowing())
            activity.runOnUiThread(() -> alertDialog.dismiss());
        if (activity == null || activity.isDestroyed())
            return false;

        activity.runOnUiThread(() -> {
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
        });

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
                    /*if (signDialog != null && signDialog.isShowing())
                        signDialog.dismiss();*/
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

        boolean success = storeEncryptedBytes(newPassword, true, currentWallet.address);  //because we'll now only ever be importing keystore, always create with Auth if possible

        if (!success)
        {
            AuthorisationFailMessage(context.getString(R.string.please_enable_security));
        }
        else
        {
            switch (operation)
            {
                case CREATE_KEYSTORE_KEY:
                    importCallback.walletValidated(new String(newPassword), KeyEncodingType.KEYSTORE_KEY, authLevel);
                    break;
                case CREATE_PRIVATE_KEY:
                    importCallback.walletValidated(new String(newPassword), KeyEncodingType.RAW_HEX_KEY, authLevel);
                    break;
            }
        }
    }

    private synchronized SignatureFromKey signWithKeystore(byte[] transactionBytes)
    {
        //1. get password from store
        //2. construct credentials
        //3. sign
        SignatureFromKey returnSig = new SignatureFromKey();
        returnSig.signature = FAILED_SIGNATURE.getBytes();
        returnSig.sigType = SignatureReturnType.KEY_AUTHENTICATION_ERROR;

        try
        {
            String password = "";
            switch (currentWallet.type)
            {
                default:
                case KEYSTORE:
                    password = unpackMnemonic();
                    break;
                case KEYSTORE_LEGACY:
                    password = new String(getLegacyPassword(context, currentWallet.address));
                    break;
            }

            File keyFolder = new File(context.getFilesDir(), KEYSTORE_FOLDER);
            Credentials credentials = KeystoreAccountService.getCredentials(keyFolder, currentWallet.address, password);
            Sign.SignatureData signatureData = Sign.signMessage(
                    transactionBytes, credentials.getEcKeyPair());
            returnSig.signature = KeystoreAccountService.bytesFromSignature(signatureData);
            returnSig.sigType = SignatureReturnType.SIGNATURE_GENERATED; //only reach here if signature was generated correctly
        }
        catch (ServiceErrorException e)
        {
            //Legacy keystore error
            if (!BuildConfig.DEBUG) analyticsService.recordException(e);
            returnSig.failMessage = e.getMessage();
            e.printStackTrace();
        }
        catch (Exception e)
        {
            returnSig.failMessage = e.getMessage();
            e.printStackTrace();
        }

        return returnSig;
    }

    /*
            Utility methods
     */

    public static byte[] readBytesFromFile(String path)
    {
        byte[] bytes = null;
        File file = new File(path);
        try (FileInputStream fin = new FileInputStream(file))
        {
            bytes = readBytesFromStream(fin);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return bytes;
    }

    public static byte[] readBytesFromStream(InputStream in) throws IOException
    {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 2048;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = in.read(buffer)) != -1)
        {
            byteBuffer.write(buffer, 0, len);
        }

        byteBuffer.close();
        return byteBuffer.toByteArray();
    }

    /**
     * Finds matching key in keystore regardless of case
     *
     * @param keyAddress
     * @return
     */
    private String findMatchingAddrInKeyStore(String keyAddress)
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            Enumeration<String> keys = keyStore.aliases();

            while (keys.hasMoreElements())
            {
                String thisKey = keys.nextElement();
                if (keyAddress.equalsIgnoreCase(thisKey))
                {
                    return thisKey;
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return keyAddress;
    }

    public synchronized static String getFilePath(Context context, String fileName)
    {
        //check for matching file
        File check = new File(context.getFilesDir(), fileName);
        if (check.exists())
        {
            return check.getAbsolutePath(); //quick return
        }
        else
        {
            //find matching file, ignoring case
            File[] files = context.getFilesDir().listFiles();
            for (File checkFile : files)
            {
                if (checkFile.getName().equalsIgnoreCase(fileName))
                {
                    return checkFile.getAbsolutePath();
                }
            }
        }

        return check.getAbsolutePath(); //Should never get here
    }

    private boolean writeBytesToFile(String path, byte[] data)
    {
        File file = new File(path);
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            fos.write(data);
        }
        catch (IOException e)
        {
            Timber.d(e, "Exception while writing file ");
            return false;
        }

        return true;
    }

    /**
     * Delete all traces of the key in Android keystore, encrypted bytes and iv file in private data area
     * @param keyAddress
     */
    synchronized void deleteKey(String keyAddress)
    {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            String matchingAddr = findMatchingAddrInKeyStore(keyAddress);
            if (keyStore.containsAlias(matchingAddr)) keyStore.deleteEntry(matchingAddr);
            File encryptedKeyBytes = new File(getFilePath(context, matchingAddr));
            File encryptedBytesFileIV = new File(getFilePath(context, matchingAddr + "iv"));
            if (encryptedKeyBytes.exists()) encryptedKeyBytes.delete();
            if (encryptedBytesFileIV.exists()) encryptedBytesFileIV.delete();
            deleteAccount(matchingAddr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteAccount(String address) throws Exception
    {
        String cleanedAddr = Numeric.cleanHexPrefix(address).toLowerCase();
            deleteAccountFiles(cleanedAddr);

            //Now delete database files (ie tokens, transactions and Tokenscript data for account)
            File[] contents = context.getFilesDir().listFiles();
            if (contents != null)
            {
                for (File f : contents)
                {
                    String fileName = f.getName().toLowerCase();
                    if (fileName.contains(cleanedAddr.toLowerCase()))
                    {
                        deleteRecursive(f);
                    }
                }
            }
    }

    private void deleteAccountFiles(String address) throws Exception
    {
        String cleanedAddr = Numeric.cleanHexPrefix(address);

        File keyFolder = new File(context.getFilesDir(), KEYSTORE_FOLDER);
        File[] contents = keyFolder.listFiles();
        if (contents != null)
        {
            for (File f : contents)
            {
                if (f.getName().contains(cleanedAddr))
                {
                    f.delete();
                }
            }
        }
    }

    private void deleteRecursive(File fp)
    {
        if (fp.isDirectory())
        {
            File[] contents = fp.listFiles();
            if (contents != null)
            {
                for (File child : contents)
                    deleteRecursive(child);
            }
        }

        fp.delete();
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

    private boolean requiresUnlock()
    {
        try
        {
            unpackMnemonic();
        }
        catch (Exception e)
        {
            return true;
        }

        return false;
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

    public boolean hasKeystore(String walletAddress)
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            String matchingAddr = findMatchingAddrInKeyStore(walletAddress);
            return keyStore.containsAlias(matchingAddr);
        }
        catch (KeyStoreException|NoSuchAlgorithmException|CertificateException|IOException e)
        {
            Timber.e(e);
        }

        return false;
    }

    static boolean hasStrongbox()
    {
        return securityStatus == SecurityStatus.HAS_STRONGBOX;
    }

    public enum KeyExceptionType
    {
        UNKNOWN, REQUIRES_AUTH, INVALID_CIPHER, SUCCESSFUL_DECODE, IV_NOT_FOUND, ENCRYPTED_FILE_NOT_FOUND
    }
}
