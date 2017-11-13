package com.wallet.crypto.trustapp.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Password Manager that should assist
 * in securely handling the user's password
 * for several wallet addresses.
 *
 * Created by Philipp Rieger on 21.10.17.
 *
 * ===================
 * Example of Usage:
 * ===================
 *
 * try {
 *      PasswordManager.setPassword("0x885758292fstwe", "s3cret", this);
 *      PasswordManager.setPassword("0x885710dsfe2345", "t0ps3cret", this);
 *
 *      Log.d("TAG", PasswordManager.getPassword("0x885758292fstwe", this));
 *      Log.d("TAG", PasswordManager.getPassword("0x885710dsfe2345", this));
 *
 * } catch (Exception e) { e.printStackTrace(); }
 *
 *
 */

public final class PasswordManager {

    /*
     * ============================
     *         NATIVE
     * ============================
     */

    static {
        System.loadLibrary("native-lib");
    }

    // These key and iv were compromised after being committed to the public github repo.
    // To migrate old clients, we will first attempt to decrypt using these keys.
    // TODO: remove once all 1.3.2 clients have upgraded
    private final static String legacyKey = "35TheTru5tWa11ets3cr3tK3y377123!";
    private final static String legacyIv = "8201va0184a0md8i";

    public static native String getKeyStringFromNative();
    public static native String getIvStringFromNative();


    /*
     * ============================
     *          MAIN METHODS
     * ============================
     */

    /**
     * Encrypts the password and sets it into the shared preferences.
     * @param password
     * @param context
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws InvalidAlgorithmParameterException
     */
    public static void setPassword(final String address, final String password, final Context context)
            throws NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, UnsupportedEncodingException, InvalidKeyException,
            InvalidKeySpecException, InvalidAlgorithmParameterException
    {
        // encrypt password
        SecretKey key = new SecretKeySpec(getKeyStringFromNative().getBytes("UTF-8"), "AES");
        IvParameterSpec iv = new IvParameterSpec(getIvStringFromNative().getBytes("UTF-8"));
        final byte[] encryptedPassword = encrypt(password, key, iv);

        // save in shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(address + "-pwd", Base64.encodeToString(encryptedPassword, Base64.DEFAULT));
        editor.commit();
    }

    /**
     * Retrieves the encrypted password from shared preferences, decrypts and and returns it
     * @param context
     * @return password
     * @throws NoSuchPaddingException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     * @throws InvalidAlgorithmParameterException
     */
    public static String getPassword(final String address, final Context context)
            throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException,
            InvalidAlgorithmParameterException
    {
        // get password from SharedPrefs
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final byte[] encryptedPassword = Base64.decode(sharedPreferences.getString(address + "-pwd", null), Base64.DEFAULT);

        // Attempt to decrypt using legacy key/iv to migrate old clients
        // TODO: remove once all clients have upgraded from 1.3.2
        SecretKey oldKey = new SecretKeySpec(legacyKey.getBytes("UTF-8"), "AES");
        IvParameterSpec oldIv = new IvParameterSpec(legacyIv.getBytes("UTF-8"));
        try {
            final String decryptedPassword = decrypt(encryptedPassword, oldKey, oldIv);
            return decryptedPassword;
        } catch (Exception e) {
            Log.e("PASSMAN", e.getMessage());
        }

        // If decryption fails, it is most likely because this is a new client
        // decrypt password
        SecretKey key = new SecretKeySpec(getKeyStringFromNative().getBytes("UTF-8"), "AES");
        IvParameterSpec iv = new IvParameterSpec(getIvStringFromNative().getBytes("UTF-8"));
        final String decryptedPassword = decrypt(encryptedPassword, key, iv);

        return decryptedPassword;
    }

    /*
     * ============================
     *    ENCRYPTION / DECRYPTION
     * ============================
     */

    /**
     *  Encrypts the given plain text with AES
     * @param plainText
     * @param key
     * @param iv
     * @return chiper text
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidAlgorithmParameterException
     */
    private static byte[] encrypt(final String plainText, final SecretKey key, final IvParameterSpec iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException,
            InvalidAlgorithmParameterException
    {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(plainText.getBytes("UTF-8"));

    }

    /**
     * Decrypts a given cipher text
     * @param cipherText
     * @param key
     * @param iv
     * @return plain text
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     * @throws InvalidAlgorithmParameterException
     */
    private static String decrypt(final byte[] cipherText, final SecretKey key, final IvParameterSpec iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException,
            InvalidAlgorithmParameterException
    {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return new String(cipher.doFinal(cipherText), "UTF-8");
    }

    /*
     * ============================
     *      UTILITY FUNCTIONS
     * ============================
     */

    /**
     * Generates a random initialization vector
     * @return iv
     */
    private static IvParameterSpec generateRandomIv() {
        SecureRandom random = new SecureRandom();
        byte[] ivBytes = new byte[16];
        random.nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        return iv;
    }


    /**
     * Generates a random one time pad key
     * Key size is 256 key
     * @return key
     * @throws NoSuchAlgorithmException
     */
    private static SecretKey generateKey() throws NoSuchAlgorithmException {
        final int outputKeyLength = 256;
        SecureRandom secureRandom = new SecureRandom();

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(outputKeyLength, secureRandom);
        SecretKey key = keyGenerator.generateKey();

        return key;
    }

    /**
     * Derives a key from a given passphrase (PBKDF2)
     * Key size is 256 bits
     * @param keyPhrase
     * @return key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static SecretKey generateKey(final String keyPhrase) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        KeySpec spec = new PBEKeySpec(keyPhrase.toCharArray(), salt, 65536, 256); // AES-256
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey key = keyFac.generateSecret(spec);
        return key;
    }
}
