package com.wallet.crypto.trust.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marat on 10/11/17.
 */

public class Utils {
    static final Pattern qrPattern = Pattern.compile("^ethereum:(0x[0-9a-f]{40,})([?]value=[0-9]{1,10})?");

    public static String extractAddressFromQrString(String qrString) {

        Matcher m = qrPattern.matcher(qrString);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

}
