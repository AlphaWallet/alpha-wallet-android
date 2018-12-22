package io.stormbird.wallet.entity;

import android.text.TextUtils;

import java.util.regex.Pattern;

public class Address {

    private static final Pattern ignoreCaseAddrPattern = Pattern.compile("(?i)^(0x)?[0-9a-f]{40}$");

    public final String value;

    public Address(String value) {
        this.value = value;
    }

    public static boolean isAddress(String address)
    {
        boolean isValidAddress = true;
        if (TextUtils.isEmpty(address) || !ignoreCaseAddrPattern.matcher(address).find())
        {
            return false;
        }

        try
        {
            new org.web3j.abi.datatypes.Address(address);
        }
        catch (UnsupportedOperationException e)
        {
            isValidAddress = false;
        }

        return isValidAddress;
    }
}
