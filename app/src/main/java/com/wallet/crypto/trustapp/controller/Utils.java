package com.wallet.crypto.trustapp.controller;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marat on 10/11/17.
 */

public class Utils {
    static final Pattern qrPattern = Pattern.compile("^([a-z]+:)?(0x[0-9a-f]{40})([?]value=[0-9]{1,10})?$");

    public static String extractAddressFromQrString(String qrString) {

        Matcher m = qrPattern.matcher(qrString.toLowerCase());
        if (m.find()) {
            return m.group(2);
        }
        return null;
    }

    public static String generatePassword() {
        byte bytes[] = new byte[256];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return String.valueOf(bytes);
    }
}
