package io.stormbird.wallet.service;

import android.content.Context;
import io.stormbird.wallet.entity.ServiceErrorException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

import static io.stormbird.wallet.entity.ServiceErrorException.*;
import static io.stormbird.wallet.service.KeyService.*;

public class LegacyKeystore
{
    private static final String LEGACY_CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    public static synchronized byte[] getLegacyPassword(
            final Context context,
            String keyName)
            throws ServiceErrorException
    {
        KeyStore keyStore;
        String encryptedDataFilePath = getFilePath(context, keyName);
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(keyName, null);
            if (secretKey == null) {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
                if (!fileExists) {
                    return null;/* file also not there, fine then */
                }
                throw new ServiceErrorException(
                        KEY_IS_GONE,
                        "file is present but the key is gone: " + keyName);
            }

            String keyIV = keyName + "iv";
            boolean ivExists = new File(getFilePath(context, keyIV)).exists();
            boolean aliasExists = new File(getFilePath(context, keyName)).exists();
            if (!ivExists || !aliasExists) {
                throw new ServiceErrorException(
                            IV_OR_ALIAS_NO_ON_DISK);
            }

            byte[] iv = readBytesFromFile(getFilePath(context, keyIV));
            if (iv == null || iv.length == 0) {
                throw new NullPointerException("iv is missing for " + keyName);
            }
            Cipher outCipher = Cipher.getInstance(LEGACY_CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            return readBytesFromStream(cipherInputStream);
        } catch (InvalidKeyException e) {
            throw new ServiceErrorException(INVALID_KEY);
        } catch (IOException | CertificateException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new ServiceErrorException(KEY_STORE_ERROR);
        }
    }
}
