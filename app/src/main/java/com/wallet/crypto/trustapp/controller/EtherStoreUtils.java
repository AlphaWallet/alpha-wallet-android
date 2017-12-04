package com.wallet.crypto.trustapp.controller;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.spongycastle.crypto.generators.SCrypt;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static cz.msebera.android.httpclient.protocol.HTTP.UTF_8;
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
