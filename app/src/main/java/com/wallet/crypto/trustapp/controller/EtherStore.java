package com.wallet.crypto.trustapp.controller;

import android.security.KeyChain;
import android.util.Log;

import org.ethereum.geth.Account;
import org.ethereum.geth.Accounts;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marat on 9/22/17.
 */

public class EtherStore {

    private KeyChain keyChain;
    private KeyStore ks;
    private static String TAG = "EtherStore";
    private Controller mController;

    public EtherStore(String filesDir, Controller controller) {
        mController = controller;
        ks = new KeyStore(filesDir + "/keystore", Geth.LightScryptN, Geth.LightScryptP);
        Log.d(TAG, "Created KeyStore with %s accounts".format(Long.toString(ks.getAccounts().size())));
    }

    public static int getMinGasLimit() {
        return 21000;
    }

    public static int getDefaultGasLimit() {
        return 90000;
    }

    public static int getMaxGasLimit() {
        return 300000;
    }

    // Wei
    public static long getDefaultGasPrice() {
        return 30000000000L;
    }

    // Wei
    public static long getTokenGasLimit() {
        return 144000;
    }

    // Wei
    public static long getMinGasFee() {
        return 21000000000L;
    }

    // Wei
    public static long getMaxGasFee() {
        return 100000000000000000L;
    }

    public Account createAccount(String password) throws Exception {
        Account account = ks.newAccount(password);
        return account;
    }

    public boolean hasAccounts() {
        return ks.getAccounts().size() > 0;
    }

    public boolean hasAddress(String address) {
        return ks.hasAddress(new Address(address));
    }

    public Account importKeyStore(String storeJson, String password) throws Exception {
        byte[] data = storeJson.getBytes(Charset.forName("UTF-8"));
        Account newAccount = ks.importKey(data, password, password);
        return newAccount;
    }

    public String exportAccount(Account account, String password, String new_password) throws Exception {
        byte[] data = ks.exportKey(account, password, new_password);
        return new String(data);
    }

    public void deleteAccount(String address, String password) throws Exception {
        Account account = getAccount(address);
        ks.deleteAccount(account, password);
    }

    public byte[] signTransaction(Account signer, String signerPassword, String toAddress, String wei, String limit, String price, byte[] data, long nonce) throws Exception {
        BigInt value = new BigInt(Long.decode(wei));

        BigInt gasPrice = new BigInt(0);
        gasPrice.setString(price, 10); // price, base

        Transaction tx = new Transaction(
                nonce, new Address(toAddress),
                value,
                new BigInt(Long.parseLong(limit)), // gas limit
                gasPrice,
                data); // data

        BigInt chain = new BigInt(mController.getCurrentNetwork().getChainId()); // Chain identifier of the main net

        ks.unlock(signer, signerPassword);
        Transaction signed = ks.signTx(signer, tx, chain);
        ks.lock(signer.getAddress());

        return signed.encodeRLP();
    }

    public Account getAccount(String address) throws Exception {
        Accounts accounts = ks.getAccounts();
        for (long i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getAddress().getHex().toLowerCase().equals(address.toLowerCase())) {
                return accounts.get(i);
            }
        }
        return null;
    }

    public List<Account> getAccounts() throws Exception {
        List<Account> out = new ArrayList<>();
        Accounts accounts = ks.getAccounts();

        for (long i = 0; i < accounts.size(); i++) {
            out.add(accounts.get(i));
        }
        return out;
    }
}
