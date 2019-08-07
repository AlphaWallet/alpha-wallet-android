package io.stormbird.wallet.service;

import android.annotation.SuppressLint;
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
import io.stormbird.wallet.ui.BackupKeyActivity;
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
import static io.stormbird.wallet.service.HDKeyService.Operation.*;
import static io.stormbird.wallet.service.KeystoreAccountService.KEYSTORE_FOLDER;
import static io.stormbird.wallet.service.LegacyKeystore.getLegacyPassword;

@TargetApi(23)
public class HDKeyService implements AuthenticationCallback, PinAuthenticationCallbackInterface
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
    public static final int TIME_BETWEEN_BACKUP_MILLIS = 1000 * 60 * 1; //TODO: RESTORE 30 DAYS. TESTING: 1 minute  //1000 * 60 * 60 * 24 * 30; //30 days
    public static final int TIME_BETWEEN_BACKUP_WARNING_MILLIS = 1000 * 60 * 3; //TODO: RESTORE 30 DAYS. TESTING: 1 minute  //1000 * 60 * 60 * 24 * 30; //30 days

    public enum Operation
    {
        CREATE_HD_KEY, FETCH_MNEMONIC, IMPORT_HD_KEY, SIGN_WITH_KEY, CHECK_AUTHENTICATION, SIGN_DATA,
        CREATE_NON_AUTHENTICATED_KEY, UPGRADE_HD_KEY, CREATE_KEYSTORE_KEY, UPGRADE_KEYSTORE_KEY,
        CREATE_PRIVATE_KEY, RESTORE_NON_AUTHENTICATED_KEY
    }

    public enum AuthenticationLevel
    {
        NOT_SET, TEE_NO_AUTHENTICATION, TEE_AUTHENTICATION, STRONGBOX_NO_AUTHENTICATION, STRONGBOX_AUTHENTICATION
    }

    public enum UpgradeKeyResult
    {
        REQUESTING_SECURITY, NO_SCREENLOCK, ALREADY_LOCKED, ERROR
    }

    private enum SecurityStatus
    {
        NOT_CHECKED, HAS_NO_TEE, HAS_TEE, HAS_STRONGBOX
    }

    private static final int DEFAULT_KEY_STRENGTH = 128;
    private final Activity context;

    private AuthenticationLevel authLevel;
    private String currentKey;
    private String currentAddress;
    private SignTransactionDialog signDialog;
    private AWalletAlertDialog alertDialog;
    private CreateWalletCallbackInterface callbackInterface;
    private ImportWalletCallback importCallback;
    private SignAuthenticationCallback signCallback;
    private String keystoreImportDetails;

    //TODO: Refactor into a proper service
    @SuppressLint("StaticFieldLeak")
    private static Activity topmostActivity;

    private static SecurityStatus securityStatus = SecurityStatus.NOT_CHECKED;

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

        checkSecurity();
    }

    public void createNewHDKey(CreateWalletCallbackInterface callback)
    {
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        createHDKey();
    }

    public void createKeystorePassword(String address, ImportWalletCallback callback, String keyStore, String password)
    {
        importCallback = callback;
        callback.setupAuthenticationCallback(this);
        currentKey = address;
        keystoreImportDetails = keyStore + "__" + password;
        createPassword(CREATE_KEYSTORE_KEY);
    }

    public void createPrivateKeyPassword(String address, ImportWalletCallback callback, String privateKey)
    {
        importCallback = callback;
        callback.setupAuthenticationCallback(this);
        currentKey = address;
        keystoreImportDetails = privateKey;
        createPassword(CREATE_PRIVATE_KEY);
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

    public void getMnemonic(String address, CreateWalletCallbackInterface callback)
    {
        currentKey = address;
        callbackInterface = callback;
        callback.setupAuthenticationCallback(this);
        unpackMnemonic(address, Operation.FETCH_MNEMONIC);
    }

    public void getAuthenticationForSignature(String walletAddr, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        callback.setupAuthenticationCallback(this);
        currentKey = walletAddr;
        Wallet wallet = new Wallet(walletAddr);
        wallet.checkWalletType(context);
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                signCallback.GotAuthorisation(true);
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

    public UpgradeKeyResult upgradeKeySecurity(String key, SignAuthenticationCallback callback)
    {
        signCallback = callback;
        callback.setupAuthenticationCallback(this);
        //first check we have ability to generate the key
        if (!deviceIsLocked())
            return UpgradeKeyResult.NO_SCREENLOCK;

        WalletType type = Wallet.getKeystoreType(context, key);

        try
        {
            //now check it's an unlocked key
            String existingEncryptedKey = getEncryptedFilePath(key);
            if (existingEncryptedKey != null && (existingEncryptedKey.contains(NO_AUTH_LABEL) || type == KEYSTORE_LEGACY))
            {
                Operation keyOperation;
                currentKey = null;
                currentAddress = key;

                switch (type)
                {
                    case KEYSTORE:
                        currentKey = unpackMnemonic(key, Operation.UPGRADE_HD_KEY);
                        keyOperation = Operation.UPGRADE_KEYSTORE_KEY;
                        break;
                    case KEYSTORE_LEGACY:
                        currentKey = new String(getLegacyPassword(context, key));
                        keyOperation = Operation.UPGRADE_KEYSTORE_KEY;
                        break;
                    case HDKEY:
                        currentKey = unpackMnemonic(key, Operation.UPGRADE_HD_KEY);
                        keyOperation = Operation.UPGRADE_HD_KEY;
                        break;
                    default:
                        return UpgradeKeyResult.ERROR;
                }

                if (currentKey == null)
                    return UpgradeKeyResult.ERROR;

                //now re-encode the key using authentication
                upgradeKey(keyOperation);
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
            return UpgradeKeyResult.ERROR;
        }
    }

    private void getAuthenticationForSignature()
    {
        //check unlock status
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(currentKey, null);
            String encryptedHDKeyPath = getEncryptedFilePath(currentKey);//getFilePath(context, currentKey + "hd");
            boolean fileExists = new File(encryptedHDKeyPath).exists();
            if (!fileExists || secretKey == null)
            {
                signCallback.GotAuthorisation(false);
                return;
            }
            byte[] iv = readBytesFromFile(getFilePath(context, currentKey + "iv"));
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

    //Auth must be unlocked
    synchronized byte[] signData(String key, byte[] transactionBytes)
    {
        Wallet wallet = new Wallet(key);
        wallet.checkWalletType(context);
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
                return signWithKeystore(wallet, transactionBytes);
            case KEYSTORE:
                return signWithKeystore(wallet, transactionBytes);
            case HDKEY:
                String mnemonic = unpackMnemonic(key, Operation.SIGN_DATA);
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
            currentKey = keyAddr;
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
     * Create key from imported seed phrase - note there's no need for non-authenticated backup here as user already has the key
     */
    private void importHDKey()
    {
        HDWallet newWallet = new HDWallet(currentKey, "");
        storeHDKey(newWallet, Operation.IMPORT_HD_KEY);
    }

    private void upgradeKey(Operation operation)
    {
        storeEncryptedBytes(currentAddress, currentKey.getBytes(), operation);
    }

    /**
     * Stores a generated HDWallet in the Android keystore.
     *
     * Operation is:
     * 1. determine HDKey ethereum address
     * 2. generate most secure key possible within constraints - if we're generating a 'backup key' then switch off user Auth
     * 3. attempt to encrypt the HD wallet seed using the generated key. If requires user Auth then go to 5, otherwise 4.
     * 4. Key generation successful - signal to calling process key creation or import is complete along with the new address.
     * 5. Key generation failed because we need a User Authentication event. Fire off the prompt for authentication, after which we start again at 1.
     *
     * @param newWallet
     * @param operation
     */
    private synchronized void storeHDKey(HDWallet newWallet, Operation operation)
    {
        String address = "";

        PrivateKey pk = newWallet.getKeyForCoin(CoinType.ETHEREUM);
        address = CoinType.ETHEREUM.deriveAddress(pk);

        storeEncryptedBytes(address, newWallet.mnemonic().getBytes(), operation);
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

            if (keyStore.containsAlias(address)) //re-import existing key - no harm done as address is generated from mnemonic
            {
                deleteKey(keyStore, address);
            }

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

            //blank secret key unless we just restored it (to handle the event that requested upgrade but then cancelled or failed authentication)
            if (operation != RESTORE_NON_AUTHENTICATED_KEY) currentKey = null;

            switch (operation)
            {
                case CREATE_HD_KEY:
                    if (callbackInterface != null)
                        callbackInterface.HDKeyCreated(address, context, authLevel);
                    break;
                case IMPORT_HD_KEY:
                    importCallback.WalletValidated(address, authLevel);
                    deleteNonAuthKey(address); //in the case the user re-imported a key, destroy the backup key
                    break;
                case UPGRADE_HD_KEY:
                    signCallback.CreatedKey(address);
                    deleteNonAuthKey(address);
                    break;
                case UPGRADE_KEYSTORE_KEY:
                    signCallback.CreatedKey(address);
                    deleteNonAuthKey(address);
                    break;
                case CREATE_NON_AUTHENTICATED_KEY:
                    if (callbackInterface != null)
                        callbackInterface.HDKeyCreated(address, context, authLevel);
                    break;
                case CREATE_KEYSTORE_KEY:
                    importCallback.KeystoreValidated(address, new String(data), keystoreImportDetails, authLevel);
                    keystoreImportDetails = null;
                    break;
                case CREATE_PRIVATE_KEY:
                    importCallback.KeyValidated(keystoreImportDetails, new String(data), authLevel);
                    keystoreImportDetails = null;
                    break;
                default:
                    break;
            }
            return;
        }
        catch (UserNotAuthenticatedException e)
        {
            //delete keys if created, but only if it's not an upgrade process
            if (operation == UPGRADE_HD_KEY || operation == UPGRADE_KEYSTORE_KEY)
            {
                //if upgrade was not possible, re-store encrypted bytes without password in case authentication is cancelled
                //We have to do this because the key used to encrypt the old data
                //has now been overwritten above at keyGenerator.generateKey();
                storeEncryptedBytes(address, data, RESTORE_NON_AUTHENTICATED_KEY);
            }
            else
            {
                deleteKey(keyStore, address);
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
        signDialog = new SignTransactionDialog(context, operation.ordinal());
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
                unpackMnemonic(currentKey, Operation.FETCH_MNEMONIC);
                break;
            case IMPORT_HD_KEY:
                importHDKey();
                break;
            case CHECK_AUTHENTICATION:
                getAuthenticationForSignature();
                break;
            case UPGRADE_HD_KEY:
            case UPGRADE_KEYSTORE_KEY:
                upgradeKey(operation);
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

        if (context == null || context.isDestroyed())
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
        if (context == null || context.isDestroyed())
            return false;

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

        //attempt to store this password. NB this may result in a callback firing off then re-entering here
        storeEncryptedBytes(currentKey, newPassword, operation);  //because we'll now only ever be importing keystore, always create with Auth
    }


    public void getPassword(String address, BackupKeyActivity callback)
    {
        currentKey = address;
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
            case RESTORE_NON_AUTHENTICATED_KEY:
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
            case RESTORE_NON_AUTHENTICATED_KEY:
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

    public boolean deleteHDKey(String keyAddr)
    {
        try
        {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            File hdEncryptedBytes = new File(getEncryptedFilePath(keyAddr));
            if (hdEncryptedBytes.exists() && keyStore.containsAlias(keyAddr))
            {
                deleteKey(keyStore, keyAddr);
            }
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a non-auth key
     * note that the iv and keystore key will have been replaced by the newly minted auth key
     *
     * @param address
     */
    private void deleteNonAuthKey(String address)
    {
        File nonAuthKey = new File(getFilePath(context, address + NO_AUTH_LABEL + HDKEY_LABEL));
        File nonAuthKeystore = new File(getFilePath(context, address + NO_AUTH_LABEL + KEYSTORE_LABEL));
        File legacyKey = new File(getFilePath(context, address));
        if (nonAuthKey.exists()) nonAuthKey.delete();
        if (nonAuthKeystore.exists()) nonAuthKeystore.delete();
        if (legacyKey.exists()) legacyKey.delete();
    }

    public synchronized void deleteKeystoreKey(String keyAddress)
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

    public static void setTopmostActivity(Activity activity)
    {
        topmostActivity = activity;
    }
}
