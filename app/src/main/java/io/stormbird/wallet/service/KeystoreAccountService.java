package io.stormbird.wallet.service;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.ServiceErrorException;
import io.stormbird.wallet.entity.Wallet;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.*;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.stormbird.wallet.service.MarketQueueService.sigFromByteArray;

public class KeystoreAccountService implements AccountKeystoreService {
    private static final int PRIVATE_KEY_RADIX = 16;

    private final File keyFolder;

    public KeystoreAccountService(File keyStoreFile) {
        keyFolder = keyStoreFile;
    }

    /**
     * TODO: remove create, we only create HD keys now
     * @param password account password
     * @return
     */
    @Override
    public Single<Wallet> createAccount(String password) {
        //return Single.fromCallable(() -> new Wallet(keyStore.newAccount(password).getAddress().getHex().toLowerCase()))
        return Single.fromCallable(() -> new Wallet("0x000"))
        .subscribeOn(Schedulers.io());
    }

    /**
     * TODO: handle import keystore
     * @param store store to include
     * @param password store password
     * @param newPassword
     * @return
     */
    @Override
    public Single<Wallet> importKeystore(String store, String password, String newPassword) {
        return Single.fromCallable(() -> {
            String address = extractAddressFromStore(store);
            if (hasAccount(address)) {
                throw new ServiceErrorException(C.ErrorCode.ALREADY_ADDED, "Already added");
            }

            try {
                //account = keyStore
                //        .importKey(store.getBytes(Charset.forName("UTF-8")), password, newPassword);
            } catch (Exception ex) {
                // We need to make sure that we do not have a broken account
                deleteAccount(address, newPassword).subscribe(() -> {}, t -> {});
                throw ex;
            }
            //return new Wallet(account.getAddress().getHex().toLowerCase());
            return new Wallet("0x000");
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
     * TODO: Import private key to keystore
     * @param privateKey
     * @param newPassword
     * @return
     */
    @Override
    public Single<Wallet> importPrivateKey(String privateKey, String newPassword) {
        return Single.fromCallable(() -> {
                                       BigInteger key = new BigInteger(privateKey, PRIVATE_KEY_RADIX);
                                       ECKeyPair keypair = ECKeyPair.create(key);
                                       return new Wallet("0x000");
                                   });
            //WalletFile walletFile = create(newPassword, keypair, N, P);
            //return new ObjectMapper().writeValueAsString(walletFile);
        //}).compose(upstream -> importKeystore(upstream.blockingGet(), newPassword, newPassword));
    }

    @Override
    public Single<String> exportAccount(Wallet wallet, String password, String newPassword) {
        return Single
                .fromCallable(() -> getCredentials(wallet.address, password))
                .map(credentials -> WalletUtils.generateWalletFile(password, credentials.getEcKeyPair(), null, true))
                .subscribeOn(Schedulers.io());
    }

    /**
     * TODO: Delete file, check if corresponding key is deleted
     * @param address account address
     * @param password account password
     * @return
     */
    @Override
    public Completable deleteAccount(String address, String password) {
        return Completable.fromAction(() -> { int i = 0; } );
//        return Single.fromCallable(() -> address + password
//                //.flatMapCompletable(account -> Completable.fromAction(
//                        //() -> keyStore.deleteAccount(account, password)))
//                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<byte[]> signTransaction(Wallet signer, String signerPassword, String toAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId) {
        return Single.fromCallable(() -> {
            String dataStr = data != null ? Numeric.toHexString(data) : "";

            RawTransaction rtx = RawTransaction.createTransaction(
                    BigInteger.valueOf(nonce),
                    gasPrice,
                    gasLimit,
                    toAddress,
                    amount,
                    dataStr
                    );

            byte[] encodedTx;
            switch (signer.type)
            {
                default:
                case NOT_DEFINED:
                case KEYSTORE:
                    Credentials credentials = getCredentials(signer.address, signerPassword);
                    encodedTx = TransactionEncoder.signMessage(rtx, credentials);
                    break;
                case HDKEY:
                    //we have already unlocked the auth here, if required
                    byte[] signData = TransactionEncoder.encode(rtx);
                    HDKeyService svs = new HDKeyService(null);
                    byte[] signatureBytes = svs.signData(signer.address, signData);
                    Sign.SignatureData sigData = sigFromByteArray(signatureBytes);
                    encodedTx = encode(rtx, sigData);
                    break;
            }

            return encodedTx;
        })
        .subscribeOn(Schedulers.io());
    }

    /**
     * Get web3j credentials
     * @param address
     * @param password
     * @return
     */
    private Credentials getCredentials(String address, String password)
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
            e.printStackTrace();
        }

        return credentials;
    }

    private static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        return RlpEncoder.encode(rlpList);
    }

    static List<RlpType> asRlpValues(
            RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> result = new ArrayList<>();

        result.add(RlpString.create(rawTransaction.getNonce()));
        result.add(RlpString.create(rawTransaction.getGasPrice()));
        result.add(RlpString.create(rawTransaction.getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(rawTransaction.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(RlpString.create(data));

        if (signatureData != null) {
            result.add(RlpString.create(signatureData.getV()));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        return result;
    }

    private Single<byte[]> encodeTransaction(byte[] signatureBytes, RawTransaction rtx)
    {
        return Single.fromCallable(() -> {
            Sign.SignatureData sigData = sigFromByteArray(signatureBytes);
            return encode(rtx, sigData);
        });
    }

    @Override
    public Single<byte[]> signTransactionFast(Wallet signer, String signerPassword, byte[] message, long chainId) {
        return Single.fromCallable(() -> {
            Credentials credentials = getCredentials(signer.address, signerPassword);
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
    public Single<byte[]> signTransaction(Wallet signer, String signerPassword, byte[] message, long chainId)
    {
        return Single.fromCallable(() -> {
            byte[] messageHash = Hash.sha3(message);
            //BigInt chain = new BigInt(chainId); // Chain identifier of the main net
            byte[] signed;
            switch (signer.type)
            {
                default:
                case NOT_DEFINED:
                case KEYSTORE:
                    Credentials credentials = getCredentials(signer.address, signerPassword);
                    Sign.SignatureData signatureData = Sign.signMessage(
                            messageHash, credentials.getEcKeyPair(), false);
                    signed = bytesFromSignature(signatureData);
                    break;
                case HDKEY:
                    HDKeyService svs = new HDKeyService(null); //sign should already be unlocked
                    signed = svs.signData(signer.address, messageHash);
                    break;
            }

            signed = patchSignatureVComponent(signed);
            return signed;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public boolean hasAccount(String address) {
        address = Numeric.cleanHexPrefix(address);
        File[] contents = keyFolder.listFiles();
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
            //UTC--2019-04-01T00-34-39.241050147Z--981d04a70689b9a47975bb86bb61568bb9b4bac0
            Wallet[] result = new Wallet[contents.length];
            int i = 0;
            for (File f : contents)
            {
                String fName = f.getName();
                int index = fName.lastIndexOf("-");
                String address = "0x" + fName.substring(index + 1);
                if (WalletUtils.isValidAddress(address))
                {
                    result[i] = new Wallet(address);
                    i++;
                }
            }

            return result;
        })
        .subscribeOn(Schedulers.io());
    }

    private byte[] patchSignatureVComponent(byte[] signature)
    {
        if (signature != null && signature.length == 65 && signature[64] < 27)
        {
            signature[64] = (byte)(signature[64] + (byte)0x1b);
        }

        return signature;
    }

    private byte[] bytesFromSignature(Sign.SignatureData signature)
    {
        byte[] sigBytes = new byte[65];
        Arrays.fill(sigBytes, (byte) 0);

        try
        {
            System.arraycopy(signature.getR(), 0, sigBytes, 0, 32);
            System.arraycopy(signature.getS(), 0, sigBytes, 32, 32);
            sigBytes[64] = signature.getV();
        }
        catch (IndexOutOfBoundsException e)
        {
            e.printStackTrace();
        }

        return sigBytes;
    }
}
