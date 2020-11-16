package com.alphawallet.app;

import androidx.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

/**
 * Created by marat on 11/22/17.
 */

@RunWith(AndroidJUnit4.class)
public class ImportPrivateKey {
    /*
    Keystore
    {"address":"7d788fc8df7165b11a19f201558fcc3590fd8d97","crypto":{"cipher":"aes-128-ctr","ciphertext":"716c1aeeb9237c925d9b4ec63360e5541030a64e2a1a8fb91a3fb703acb20cd8","cipherparams":{"iv":"0d19830ebc74ed223442daa5d0e67912"},"kdf":"scrypt","kdfparams":{"dklen":32,"n":4096,"p":6,"r":8,"salt":"eb3017442d9edcfb2f05185603aa66fa635ad99284b7ac55764928db5de461ca"},"mac":"a0ad032bac0c2ad62d4eca2bf50f7814fbef91b78e5b29b025cc4f8a685902c5"},"id":"c0c9d734-147b-4df0-abe8-b5bcd54e6e96","version":3}
    Private key
    68adf89afe85baa046919f904f7c1e3a9cb28ca8b3039c2bcb3fa5a980d3a165
    */

//    @Test
//    public void privateKeyToKeystoreTest() throws UnsupportedEncodingException, CipherException, JsonProcessingException {
//        String privateKey = "68adf89afe85baa046919f904f7c1e3a9cb28ca8b3039c2bcb3fa5a980d3a165";
//        String passphrase = "x";
//        WalletFile w = EtherStoreUtils.convertPrivateKeyToKeystoreFile(privateKey, passphrase);
//
//        assert(w.getFirstAddress().equals("7d788fc8df7165b11a19f201558fcc3590fd8d97"));
//    }
}
