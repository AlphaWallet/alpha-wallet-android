package com.wallet.crypto.alphawallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ServiceErrorException;
import com.wallet.crypto.alphawallet.entity.ServiceException;
import com.wallet.crypto.alphawallet.entity.Wallet;

import org.ethereum.geth.Account;
import org.ethereum.geth.Accounts;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.WalletFile;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static org.web3j.crypto.Wallet.create;

public class GethKeystoreAccountService implements AccountKeystoreService {
    private static final int PRIVATE_KEY_RADIX = 16;
    /**
     * CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than 2^(128 * r / 8).
     */
    private static final int N = 1 << 9;
    /**
     * Parallelization parameter. Must be a positive integer less than or equal to Integer.MAX_VALUE / (128 * r * 8).
     */
    private static final int P = 1;

    private final KeyStore keyStore;

    public GethKeystoreAccountService(File keyStoreFile) {
        keyStore = new KeyStore(keyStoreFile.getAbsolutePath(), Geth.LightScryptN, Geth.LightScryptP);
    }

    public GethKeystoreAccountService(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public Single<Wallet> createAccount(String password) {
        return Single.fromCallable(() -> new Wallet(keyStore.newAccount(password).getAddress().getHex().toLowerCase()))
        .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Wallet> importKeystore(String store, String password, String newPassword) {
        return Single.fromCallable(() -> {
            String address = extractAddressFromStore(store);
            if (hasAccount(address)) {
                throw new ServiceErrorException(C.ErrorCode.ALREADY_ADDED, "Already added");
            }
            Account account;
            try {
                account = keyStore
                        .importKey(store.getBytes(Charset.forName("UTF-8")), password, newPassword);
            } catch (Exception ex) {
                // We need to make sure that we do not have a broken account
                deleteAccount(address, newPassword).subscribe(() -> {}, t -> {});
                throw ex;
            }
            return new Wallet(account.getAddress().getHex().toLowerCase());
        }).subscribeOn(Schedulers.io());
    }

    private String extractAddressFromStore(String store) throws Exception {
        try {
            JSONObject jsonObject = new JSONObject(store);
            return "0x" + jsonObject.getString("address");
        } catch (JSONException ex) {
            throw new Exception("Invalid keystore");
        }
    }

    @Override
    public Single<Wallet> importPrivateKey(String privateKey, String newPassword) {
        return Single.fromCallable(() -> {
            BigInteger key = new BigInteger(privateKey, PRIVATE_KEY_RADIX);
            ECKeyPair keypair = ECKeyPair.create(key);
            WalletFile walletFile = create(newPassword, keypair, N, P);
            return new ObjectMapper().writeValueAsString(walletFile);
        }).compose(upstream -> importKeystore(upstream.blockingGet(), newPassword, newPassword));
    }

    @Override
    public Single<String> exportAccount(Wallet wallet, String password, String newPassword) {
        return Single
                .fromCallable(() -> findAccount(wallet.address))
                .flatMap(account1 -> Single.fromCallable(()
                        -> new String(keyStore.exportKey(account1, password, newPassword))))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Completable deleteAccount(String address, String password) {
        return Single.fromCallable(() -> findAccount(address))
                .flatMapCompletable(account -> Completable.fromAction(
                        () -> keyStore.deleteAccount(account, password)))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<byte[]> signTransaction(Wallet signer, String signerPassword, String toAddress, BigInteger amount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId) {
        return Single.fromCallable(() -> {
            BigInt value = new BigInt(0);
            value.setString(amount.toString(), 10);

            BigInt gasPriceBI = new BigInt(0);
            gasPriceBI.setString(gasPrice.toString(), 10);

            BigInt gasLimitBI = new BigInt(0);
            gasLimitBI.setString(gasLimit.toString(), 10);

            Transaction tx = new Transaction(
                    nonce,
                    new Address(toAddress),
                    value,
                    gasLimitBI,
                    gasPriceBI,
                    data);

            BigInt chain = new BigInt(chainId); // Chain identifier of the main net
            org.ethereum.geth.Account gethAccount = findAccount(signer.address);
            keyStore.unlock(gethAccount, signerPassword);
            Transaction signed = keyStore.signTx(gethAccount, tx, chain);
            keyStore.lock(gethAccount.getAddress());

            return signed.encodeRLP();
        })
        .subscribeOn(Schedulers.io());
    }

    @Override
    public void unlockAccount(Wallet signer, String signerPassword) throws Exception {
        org.ethereum.geth.Account gethAccount = findAccount(signer.address);
        keyStore.unlock(gethAccount, signerPassword);
    }

    @Override
    public void lockAccount(Wallet signer, String signerPassword) throws Exception {
        org.ethereum.geth.Account gethAccount = findAccount(signer.address);
        keyStore.lock(gethAccount.getAddress());
    }

    @Override
    public Single<byte[]> signTransactionFast(Wallet signer, String signerPassword, byte[] message, long chainId) {
        return Single.fromCallable(() -> {
            byte[] messageHash = Hash.sha3(message);
            org.ethereum.geth.Account gethAccount = findAccount(signer.address);
            byte[] signed = keyStore.signHash(gethAccount.getAddress(), messageHash);
            return signed;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<byte[]> signTransaction(Wallet signer, String signerPassword, byte[] message, long chainId)
    {
        return Single.fromCallable(() -> {
            byte[] messageHash = Hash.sha3(message);
            BigInt chain = new BigInt(chainId); // Chain identifier of the main net
            org.ethereum.geth.Account gethAccount = findAccount(signer.address);
            keyStore.unlock(gethAccount, signerPassword);
            byte[] signed = keyStore.signHash(gethAccount.getAddress(), messageHash);
            keyStore.lock(gethAccount.getAddress());
            return signed;
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public boolean hasAccount(String address) {
        return keyStore.hasAddress(new Address(address));
    }

    @Override
    public Single<Wallet[]> fetchAccounts() {
        return Single.fromCallable(() -> {
            Accounts accounts = keyStore.getAccounts();
            int len = (int) accounts.size();
            Wallet[] result = new Wallet[len];

            for (int i = 0; i < len; i++) {
                org.ethereum.geth.Account gethAccount = accounts.get(i);
                result[i] = new Wallet(gethAccount.getAddress().getHex().toLowerCase());
            }
            return result;
        })
        .subscribeOn(Schedulers.io());
    }

    private org.ethereum.geth.Account findAccount(String address) throws ServiceException {
        Accounts accounts = keyStore.getAccounts();
        int len = (int) accounts.size();
        for (int i = 0; i < len; i++) {
            try {
                android.util.Log.d("ACCOUNT_FIND", "Address: " + accounts.get(i).getAddress().getHex());
                if (accounts.get(i).getAddress().getHex().equalsIgnoreCase(address)) {
                    return accounts.get(i);
                }
            } catch (Exception ex) {
                /* Quietly: interest only result, maybe next is ok. */
            }
        }
        throw new ServiceException("Wallet with address: " + address + " not found");
    }
}
