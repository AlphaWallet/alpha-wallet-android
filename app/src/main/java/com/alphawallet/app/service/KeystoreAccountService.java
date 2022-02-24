package com.alphawallet.app.service;

import android.util.Log;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.cryptokeys.SignatureReturnType;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.Signable;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;

public class KeystoreAccountService implements AccountKeystoreService
{
    public static final String KEYSTORE_FOLDER = "keystore/keystore";
    private static final int PRIVATE_KEY_RADIX = 16;

    private final File keyFolder;
    private final File databaseFolder;
    private final KeyService keyService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public KeystoreAccountService(File keyStoreFile, File baseFile, KeyService keyService) {
        keyFolder = keyStoreFile;
        databaseFolder = baseFile;
        this.keyService = keyService;
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //ensure keystore Folder is created
        if (!keyStoreFile.exists())
        {
            keyStoreFile.mkdirs();
        }
    }

    /**
     * No longer used; keep for testing
     * @param password account password
     * @return
     */
    @Override
    public Single<Wallet> createAccount(String password) {
        return Single.fromCallable(() -> {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, ecKeyPair);
            return objectMapper.writeValueAsString(walletFile);
        }).compose(upstream -> importKeystore(upstream.blockingGet(), password, password))
        .subscribeOn(Schedulers.io());
    }

    /**
     * Import Keystore
     * @param store store to include
     * @param password store password
     * @param newPassword
     * @return
     */
    @Override
    public Single<Wallet> importKeystore(String store, String password, String newPassword) {
        return Single.fromCallable(() -> {
            String address = extractAddressFromStore(store);
            Wallet wallet;
            //delete old account files - these have had their password overwritten. If present user chose to refresh key
            deleteAccountFiles(address);

            try {
                WalletFile walletFile = objectMapper.readValue(store, WalletFile.class);
                ECKeyPair kp = org.web3j.crypto.Wallet.decrypt(password, walletFile);
                Credentials credentials = Credentials.create(kp);
                WalletFile wFile = org.web3j.crypto.Wallet.createLight(newPassword, credentials.getEcKeyPair());

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh-mm-ss.mmmm'Z'", Locale.ROOT);
                String asString = formatter.format(System.currentTimeMillis());

                String fileName = "UTC--" + asString.replace(":", "-") + "--" + Numeric.cleanHexPrefix(credentials.getAddress());
                //write new keystore to file
                File destination = new File(keyFolder, fileName);
                objectMapper.writeValue(destination, wFile);

                wallet = new Wallet(credentials.getAddress());
                wallet.setWalletType(WalletType.KEYSTORE);
            } catch (Exception ex) {
                // We need to make sure that we do not have a broken account
                deleteAccount(address, newPassword).subscribe(() -> {}, t -> {}).isDisposed();
                throw ex;
            }

            return wallet;
        }).subscribeOn(Schedulers.io());
    }

    private String extractAddressFromStore(String store) throws Exception {
        try {
            JSONObject jsonObject = new JSONObject(store);
            return "0x" + Numeric.cleanHexPrefix(jsonObject.getString("address"));
        } catch (JSONException ex) {
            throw new Exception("Invalid keystore");
        }
    }

    /**
     * Import private key to keystore
     *
     * @param privateKey
     * @param newPassword
     * @return
     */
    @Override
    public Single<Wallet> importPrivateKey(String privateKey, String newPassword) {
        return Single.fromCallable(() -> {
            BigInteger key = new BigInteger(privateKey, PRIVATE_KEY_RADIX);
            ECKeyPair keypair = ECKeyPair.create(key);
            WalletFile wFile = org.web3j.crypto.Wallet.createLight(newPassword, keypair);
            return objectMapper.writeValueAsString(wFile);
        }).compose(upstream -> importKeystore(upstream.blockingGet(), newPassword, newPassword));
    }

    @Override
    public Single<String> exportAccount(Wallet wallet, String password, String newPassword) {
        return Single
                .fromCallable(() -> getCredentials(keyFolder, wallet.address, password))
                .map(credentials -> org.web3j.crypto.Wallet.createLight(newPassword, credentials.getEcKeyPair()))
                .map(objectMapper::writeValueAsString)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Delete 'geth' keystore file then ensure password encrypted bytes and keys in Android keystore
     * are deleted
     * @param address account address
     * @param password account password
     * @return
     */
    @Override
    public Completable deleteAccount(String address, String password) {
        return Completable.fromAction(() -> {
            String cleanedAddr = Numeric.cleanHexPrefix(address).toLowerCase();
            deleteAccountFiles(cleanedAddr);

            //Now delete database files (ie tokens, transactions and Tokenscript data for account)
            File[] contents = databaseFolder.listFiles();
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

            //Now delete all traces of the key in Android keystore, encrypted bytes and iv file in private data area
            keyService.deleteKey(address);
        } );
    }

    private void deleteAccountFiles(String address)
    {
        String cleanedAddr = Numeric.cleanHexPrefix(address);
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

    @Override
    public Single<SignatureFromKey> signTransaction(Wallet signer, String toAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId) {
        return Single.fromCallable(() -> {
            SignatureFromKey returnSig = new SignatureFromKey();
            Sign.SignatureData sigData;
            String dataStr = data != null ? Numeric.toHexString(data) : "";

            RawTransaction rtx = RawTransaction.createTransaction(
                    BigInteger.valueOf(nonce),
                    gasPrice,
                    gasLimit,
                    toAddress,
                    amount,
                    dataStr
                    );

            byte[] signData = TransactionEncoder.encode(rtx, chainId);
            returnSig = keyService.signData(signer, signData);
            sigData = sigFromByteArray(returnSig.signature);
            if (sigData == null) {
                returnSig.sigType = SignatureReturnType.KEY_CIPHER_ERROR;
                returnSig.failMessage = "Incorrect signature length"; //should never see this message
            }
            else sigData = TransactionEncoder.createEip155SignatureData(sigData, chainId);
            returnSig.signature = encode(rtx, sigData);
            return returnSig;
        })
        .subscribeOn(Schedulers.io());
    }

    /**
     * Get web3j credentials
     * @param keyFolder KeyStore Folder
     * @param address
     * @param password
     * @return
     */
    public static Credentials getCredentials(File keyFolder, String address, String password)
    {
        Credentials credentials = null;
        //first find the file
        try
        {
            address = Numeric.cleanHexPrefix(address);
            File[] contents = keyFolder.listFiles();
            for (File f : contents)
            {
                if (f.getName().contains(address))
                {
                    credentials = WalletUtils.loadCredentials(password, f);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        Timber.tag("RealmDebug").d("gotcredentials + %s", address);
        return credentials;
    }

    private static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = TransactionEncoder.asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    @Override
    public Single<byte[]> signTransactionFast(Wallet signer, String signerPassword, byte[] message, long chainId) {
        return Single.fromCallable(() -> {
            Credentials credentials = getCredentials(keyFolder, signer.address, signerPassword);
            Sign.SignatureData signatureData = Sign.signMessage(
                    message, credentials.getEcKeyPair());
            byte[] signed = bytesFromSignature(signatureData);
            signed = patchSignatureVComponent(signed);
            return signed;
        }).subscribeOn(Schedulers.io());
    }

    //In all cases where we need to sign data the signature needs to be in Ethereum format
    //Geth gives us the pure EC function, but for hash signing
    @Override
    public Single<SignatureFromKey> signTransaction(Wallet signer, byte[] message, long chainId)
    {
        return Single.fromCallable(() -> {
            //byte[] messageHash = Hash.sha3(message);
            SignatureFromKey returnSig = keyService.signData(signer, message);
            returnSig.signature = patchSignatureVComponent(returnSig.signature);
            return returnSig;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<SignatureFromKey> signMessage(Wallet signer, Signable message, long chainId)
    {
        return Single.fromCallable(() -> {
            //byte[] messageHash = Hash.sha3(message);
            SignatureFromKey returnSig = keyService.signData(signer, message.getPrehash());
            returnSig.signature = patchSignatureVComponent(returnSig.signature);
            return returnSig;
        });
    }

    @Override
    public boolean hasAccount(String address) {
        address = Numeric.cleanHexPrefix(address);
        File[] contents = keyFolder.listFiles();
        if (contents == null) return false;
        for (File f : contents)
        {
            if (f.getName().contains(address))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Single<Wallet[]> fetchAccounts() {
        return Single.fromCallable(() -> {
            File[] contents = keyFolder.listFiles();
            List<Date> fileDates = new ArrayList<>();
            Map<Date, String> walletMap = new HashMap<>();
            List<Wallet> wallets = new ArrayList<>();
            if (contents == null || contents.length == 0) return new Wallet[0];
            //Wallet[] result = new Wallet[contents.length];
            for (File f : contents)
            {
                String fName = f.getName();
                int index = fName.lastIndexOf("-");
                String address = "0x" + fName.substring(index + 1);
                if (Utils.isAddressValid(address))
                {
                    String d = fName.substring(5, index-1).replace("T", " ").substring(0, 23);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS", Locale.ROOT);
                    Date date = simpleDateFormat.parse(d);
                    fileDates.add(date);
                    walletMap.put(date, address);
                }
            }

            Collections.sort(fileDates);

            //now build a date sorted array:
            for (Date d : fileDates)
            {
                String address = walletMap.get(d);
                Wallet wallet = new Wallet(address);
                wallet.type = WalletType.KEYSTORE;
                wallet.walletCreationTime = d.getTime();
                wallets.add(wallet);
            }

            return wallets.toArray(new Wallet[0]);
        })
        .subscribeOn(Schedulers.io());
    }

    /**
     * Patch the 'V'/position component of the signature bytes. The spongy castle signing algorithm returns
     * 0 or 1 for the V component, and although most services accept this some require V to be 27 or 28
     * This just changes 0 or 1 to 0x1b or 0x1c to be universally compatible with all ethereum services.
     * Simple test example: login to 'Chibi Fighters' Dapp (in the discover dapps in the wallet)
     * by signing their challenge. With V = 0 or 1 challenge (ie without this patch)
     * verification will fail, but will pass with V = 0x1b or 0x1c.
     *
     * @param signature
     * @return
     */
    private byte[] patchSignatureVComponent(byte[] signature)
    {
        if (signature != null && signature.length == 65 && signature[64] < 27)
        {
            signature[64] = (byte)(signature[64] + (byte)0x1b);
        }

        return signature;
    }

    public static byte[] bytesFromSignature(Sign.SignatureData signature)
    {
        byte[] sigBytes = new byte[65];
        Arrays.fill(sigBytes, (byte) 0);

        try
        {
            System.arraycopy(signature.getR(), 0, sigBytes, 0, 32);
            System.arraycopy(signature.getS(), 0, sigBytes, 32, 32);
            System.arraycopy(signature.getV(), 0, sigBytes, 64, 1);
        }
        catch (IndexOutOfBoundsException e)
        {
            Timber.e(e);
        }

        return sigBytes;
    }
}
