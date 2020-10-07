package com.alphawallet.app.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.web3j.crypto.Keys.ADDRESS_LENGTH_IN_HEX;

public class VelasUtils {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final HashMap<Character, Integer> ALPHABET_MAP = new HashMap<>();
    private static final int BASE = 58;
    private static final double BITS_PER_DIGIT = Math.log(BASE) / Math.log(2);

    static int maxEncodedLen(int n) {
        return (int) Math.ceil(n / BITS_PER_DIGIT);
    }

    public static boolean isValidVlxAddress(String input) {
        if (TextUtils.isEmpty(input)) {
            return false;
        }
        return ((input.startsWith("V") || input.startsWith("v")) && input.length() == 34);
    }

    public static String toHexString(byte[] hash) {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    static String sha256(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(string.getBytes(StandardCharsets.UTF_8));
            return Hex.cleanHexPrefix(Hex.byteArrayToHexString(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String ethToVlx(String hexAddress) throws Error {
        String cleanAddress = Hex.cleanHexPrefix(hexAddress).toLowerCase();
        if (TextUtils.isEmpty(cleanAddress) || cleanAddress.length() != 40) {
            throw new Error("Invalid address length");
        }
        String checksum = sha256(sha256(cleanAddress)).substring(0, 8);
        String longAddress = cleanAddress + checksum;
        int[] buffer = hexStringToByteArray(longAddress);
        if (buffer.length == 0) {
            throw new Error("Invalid address");
        }
        ArrayList<Integer> digits = new ArrayList<>();
        digits.add(0);
        for (int i = 0; i < buffer.length; i++) {
            for (int j = 0; j < digits.size(); j++) {
                digits.set(j, digits.get(j) << 8);
            }
            digits.set(0, digits.get(0) + buffer[i]);
            int carry = 0;
            for (int j = 0; j < digits.size(); ++j) {
                digits.set(j, digits.get(j) + carry);
                carry = (digits.get(j) / BASE) | 0;
                digits.set(j, digits.get(j) % BASE);
            }
            while (carry > 0) {
                digits.add(carry % BASE);
                carry = (carry / BASE) | 0;
            }
        }
        int zeros = (int) (maxEncodedLen(buffer.length * 8) - digits.size());
        for (int i = 0; i < zeros; i++) {
            digits.add(0);
        }
        StringBuilder stringBuilder = new StringBuilder();
        byte[] reverseDigits = new byte[maxEncodedLen(buffer.length * 8)];
        for (int i = 0; i < digits.size(); i++) {
            reverseDigits[i] = digits.get(digits.size() - i - 1).byteValue();
        }
        for (int i = 0; i < reverseDigits.length; i++) {
            stringBuilder.append(ALPHABET.charAt(reverseDigits[i]));
        }
        return "V" + stringBuilder.toString();
    }


    public static String vlxToEth(String vlxAddress) throws Error {
        if (!VelasUtils.isValidVlxAddress(vlxAddress)) {
            throw new Error("Invalid address");
        }
        String string = vlxAddress.substring(1);
        ArrayList<Integer> bytes = new ArrayList<>();
        bytes.add(0);
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (ALPHABET_MAP.get(c) == null) {
                throw new Error("Non-base58 character");
            }
            for (int j = 0; j < bytes.size(); j++) {
                bytes.set(j, bytes.get(j) * BASE);
            }
            bytes.set(0, bytes.get(0) + ALPHABET_MAP.get(c));

            int carry = 0;
            for (int j = 0; j < bytes.size(); ++j) {
                bytes.set(j, bytes.get(j) + carry);
                carry = bytes.get(j) >> 8;
                bytes.set(j, bytes.get(j) & 0xff);
            }
            while (carry > 0) {
                bytes.add(carry & 0xff);
                carry = carry >> 8;
            }
        }
        int zeros = 24 - bytes.size();
        for (int i = 0; i < zeros; i++) {
            bytes.add(0);
        }

        byte[] reverseBytes = new byte[24];
        for (int i = 0; i < bytes.size(); i++) {
            reverseBytes[i] = bytes.get(bytes.size() - i - 1).byteValue();
        }
        String longAddress = Hex.cleanHexPrefix(Hex.byteArrayToHexString(reverseBytes));
        if (TextUtils.isEmpty(longAddress) || longAddress.length() != 48) {
            throw new Error("Invalid address length");
        }
        String ethAddress = longAddress.substring(0, 40);
        String addressChecksum = longAddress.substring(40);
        if (TextUtils.isEmpty(ethAddress) || TextUtils.isEmpty(addressChecksum)) {
            throw new Error("Invalid address");
        }
        String checksum = sha256(sha256(ethAddress)).substring(0, 8);
        if (!addressChecksum.equalsIgnoreCase(checksum)) {
            throw new Error("Invalid address");
        }
        return "0x" + ethAddress;
    }

    @NonNull
    private static int[] hexStringToByteArray(@Nullable String input) {
        String cleanInput = Hex.cleanHexPrefix(input);
        if (TextUtils.isEmpty(cleanInput)) {
            return new int[]{};
        }
        int len = cleanInput.length();
        int[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new int[(len / 2) + 1];
            data[0] = (byte) Character.digit(cleanInput.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new int[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            byte temp = (byte) ((Character.digit(cleanInput.charAt(i), 16) << 4)
                    + Character.digit(cleanInput.charAt(i + 1), 16));
            int number = temp & 0xff;
            data[(i + 1) / 2] = number;
        }
        return data;
    }

    static {
        for (int i = 0; i < ALPHABET.length(); i++) {
            ALPHABET_MAP.put(ALPHABET.charAt(i), i);
        }
    }
}
