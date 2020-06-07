package com.alphawallet.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.alphawallet.token.entity.EthereumMessage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Hex {

    public static int hexToInteger(String input, int def) {
        Integer value = hexToInteger(input);
        return value == null ? def : value;
    }

    @Nullable
    public static Integer hexToInteger(String input) {
        try {
            return Integer.decode(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static long hexToLong(String input, int def) {
        Long value = hexToLong(input);
        return value == null ? def : value;
    }

    @Nullable
    public static Long hexToLong(String input) {
        try {
            return Long.decode(input);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Nullable
    public static BigInteger hexToBigInteger(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        try {
            boolean isHex = containsHexPrefix(input);
            if (isHex) {
                input = cleanHexPrefix(input);
            }
            return new BigInteger(input, isHex ? 16 : 10);
        } catch (NullPointerException | NumberFormatException ex) {
            return null;
        }
    }

    @NonNull
    public static BigInteger hexToBigInteger(String input, BigInteger def) {
        BigInteger value = hexToBigInteger(input);
        return value == null ? def : value;
    }

    @Nullable
    public static BigDecimal hexToBigDecimal(String input) {
        return new BigDecimal(hexToBigInteger(input));
    }

    @NonNull
    public static BigDecimal hexToBigDecimal(String input, BigDecimal def) {
        return new BigDecimal(hexToBigInteger(input, def.toBigInteger()));
    }

    public static boolean containsHexPrefix(String input) {
        return input.length() > 1 && input.charAt(0) == '0' && input.charAt(1) == 'x';
    }

    @Nullable
    public static String cleanHexPrefix(@Nullable String input) {
        if (input != null && containsHexPrefix(input)) {
            input = input.substring(2);
        }
        return input;
    }

    @Nullable
    public static String hexToDecimal(@Nullable String value) {
        BigInteger result = hexToBigInteger(value);
        return result == null ? null : result.toString(10);
    }

    @NonNull
    public static byte[] hexStringToByteArray(@Nullable String input) {
        String cleanInput = cleanHexPrefix(input);
        if (TextUtils.isEmpty(cleanInput)) {
            return new byte[] {};
        }
        int len = cleanInput.length();
        byte[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new byte[(len / 2) + 1];
            data[0] = (byte) Character.digit(cleanInput.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new byte[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            data[(i + 1) / 2] = (byte) ((Character.digit(cleanInput.charAt(i), 16) << 4)
                    + Character.digit(cleanInput.charAt(i + 1), 16));
        }
        return data;
    }

    @NonNull
    public static String byteArrayToHexString(@NonNull byte[] input, int offset, int length, boolean withPrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (withPrefix) {
            stringBuilder.append("0x");
        }
        for (int i = offset; i < offset + length; i++) {
            stringBuilder.append(String.format("%02x", input[i] & 0xFF));
        }

        return stringBuilder.toString();
    }

    @Nullable
    public static String byteArrayToHexString(byte[] input) {
        if (input == null || input.length == 0) {
            return null;
        }
        return byteArrayToHexString(input, 0, input.length, true);
    }

    /*public static String decodeMessageData(EthereumMessage message) {
        if (cleanHexPrefix(message.value).length() == 64) {
            return message.value;
        } else {
            return containsHexPrefix(message.value)
                    ? new String(hexStringToByteArray(message.value)) : message.value;
        }
    }*/

    public static String hexToUtf8(String hex) {
        hex = org.web3j.utils.Numeric.cleanHexPrefix(hex);
        ByteBuffer buff = ByteBuffer.allocate(hex.length() / 2);
        for (int i = 0; i < hex.length(); i += 2) {
            buff.put((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        buff.rewind();
        CharBuffer cb = StandardCharsets.UTF_8.decode(buff);
        return cb.toString();
    }
}
