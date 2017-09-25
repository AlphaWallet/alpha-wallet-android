package com.example.marat.wal;

import android.security.KeyChain;
import android.util.Log;

import org.ethereum.geth.Account;
import org.ethereum.geth.Address;
import org.ethereum.geth.BigInt;
import org.ethereum.geth.Geth;
import org.ethereum.geth.KeyStore;
import org.ethereum.geth.Transaction;
import org.web3j.abi.datatypes.generated.Int64;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Created by marat on 9/22/17.
 */

public class EtherStore {

    private KeyChain keyChain;

    private KeyStore ks;

    public EtherStore(String filesDir) {
        ks = new KeyStore(filesDir + "/keystore", Geth.LightScryptN, Geth.LightScryptP);
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

    public void deleteAccount(Account account, String password) throws Exception {
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
}
