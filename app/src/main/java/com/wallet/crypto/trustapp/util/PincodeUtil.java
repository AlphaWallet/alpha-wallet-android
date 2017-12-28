package com.wallet.crypto.trustapp.util;

/**
 * Created by Philipp Rieger on 28.12.17.
 */

public class PincodeUtil {

    private static PincodeUtil instance;

    private PincodeUtil() { }

    public static PincodeUtil getInstance() {
        if (instance == null) {
            instance = new PincodeUtil();
        }
        return instance;
    }

    public void askForPin() {

    }

    public void enablePincode() {

    }

    public void disablePincode() {

    }

}
