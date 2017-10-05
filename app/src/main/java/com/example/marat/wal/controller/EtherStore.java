package com.example.marat.wal.controller;

import android.security.KeyChain;
import android.util.Log;

import org.ethereum.geth.Account;
import org.ethereum.geth.Accounts;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;
import org.web3j.abi.datatypes.generated.Int64;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by marat on 9/22/17.
 */

public class EtherStore {

    private KeyChain keyChain;
    private KeyStore ks;
    private static String TAG = "EtherStore";

    public EtherStore(String filesDir) {
        ks = new KeyStore(filesDir + "/keystore", Geth.LightScryptN, Geth.LightScryptP);
        Log.d(TAG, "Created KeyStore with %s accounts".format(Long.toString(ks.getAccounts().size())));
    }

    public Account createAccount(String password) throws Exception {
        Account account = ks.newAccount(password);
        return account;
    }

    public boolean hasAccounts() {
        return ks.getAccounts().size() > 0;
    }

    public Account importKeyStore(String storeJson, String password) throws Exception {
        byte[] data = storeJson.getBytes(Charset.forName("UTF-8"));
        Account newAccount = ks.importKey(data, password, password);
        //TODO: store account password in keychain
        return newAccount;
    }

    public String exportAccount(Account account, String password) throws Exception {
        byte[] data = ks.exportKey(account, password, password);
        return new String(data);
    }

    public void deleteAccount(String address, String password) throws Exception {
        Account account = getAccount(address);
        ks.deleteAccount(account, password);
    }

    public byte[] signTransaction(Account signer, String signerPassword, String toAddress, long nonce) throws Exception {
        Transaction tx = new Transaction(
                nonce, new Address(toAddress),
                new BigInt(0), new BigInt(30000), new BigInt(0), null); // Random empty transaction

        BigInt chain = new BigInt(42); // Chain identifier of the main net

        // Sign a transaction with a single authorization
        //Transaction signed = ks.signTxPassphrase(signer, signerPassword, tx, chain);

        ks.unlock(signer, signerPassword);
        Transaction signed = ks.signTx(signer, tx, chain);
        ks.lock(signer.getAddress());

        return signed.encodeRLP();
    }

    public Account getAccount(String address) throws Exception {
        Accounts accounts = ks.getAccounts();
        for (long i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getAddress().getHex().equals(address)) {
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
