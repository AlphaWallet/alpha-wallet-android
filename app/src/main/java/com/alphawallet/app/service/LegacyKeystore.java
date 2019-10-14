package com.alphawallet.app.service;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import com.alphawallet.app.entity.ServiceErrorException;

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

import static com.alphawallet.app.entity.ServiceErrorException.*;
import static com.alphawallet.app.entity.ServiceErrorException.ServiceErrorCode.KEY_IS_GONE;

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
        String encryptedDataFilePath = KeyService.getFilePath(context, keyName);
        try
        {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(keyName, null);
            if (secretKey == null)
            {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
                if (!fileExists)
                {
                    throw new ServiceErrorException(KEY_IS_GONE, "file and key are gone: " + keyName);
                }
                else
                {
                    throw new ServiceErrorException(KEY_IS_GONE, "file is present but the key is gone: " + keyName);
                }
            }

            String keyIV = keyName + "iv";
            boolean ivExists = new File(KeyService.getFilePath(context, keyIV)).exists();
            boolean aliasExists = new File(KeyService.getFilePath(context, keyName)).exists();
            if (!ivExists || !aliasExists)
            {
                throw new ServiceErrorException(ServiceErrorCode.IV_OR_ALIAS_NO_ON_DISK);
            }

            byte[] iv = KeyService.readBytesFromFile(KeyService.getFilePath(context, keyIV));
            if (iv == null || iv.length == 0)
            {
                throw new NullPointerException("iv is missing for " + keyName);
            }
            Cipher outCipher = Cipher.getInstance(LEGACY_CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            return KeyService.readBytesFromStream(cipherInputStream);
        } catch (UserNotAuthenticatedException e) {
            throw new ServiceErrorException(ServiceErrorCode.USER_NOT_AUTHENTICATED);
        } catch (InvalidKeyException e) {
            throw new ServiceErrorException(ServiceErrorCode.INVALID_KEY);
        } catch (IOException | CertificateException | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            throw new ServiceErrorException(ServiceErrorCode.KEY_STORE_ERROR);
        }
    }
}
