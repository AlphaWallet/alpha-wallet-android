package com.wallet.crypto.trustapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import static org.web3j.crypto.Wallet.create;

/**
 * Created by marat on 11/22/17.
 */

public class EtherStoreUtils {

    private static final int N = 1 << 9;
    private static final int P = 1;

    public static WalletFile convertPrivateKeyToKeystoreFile(String privateKey, String passphrase) throws UnsupportedEncodingException, CipherException, JsonProcessingException {
        BigInteger key = new BigInteger(privateKey, 16);
        ECKeyPair keypair = ECKeyPair.create(key);
        WalletFile w = create(passphrase, keypair, N, P);
        return w;
    }

    public static String convertPrivateKeyToKeystoreJson(String privateKey, String passphrase) throws UnsupportedEncodingException, CipherException, JsonProcessingException {
        WalletFile w = convertPrivateKeyToKeystoreFile(privateKey, passphrase);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(w);
        return json;
    }
}
