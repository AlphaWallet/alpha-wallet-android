package com.wallet.crypto.trust.model;

import java.math.BigInteger;

/**
 * Created by marat on 9/26/17.
 */

public class VMAccount {
    private String mAddress;
    private BigInteger mBalanceInWei;

    public VMAccount(String mAddress, String balanceInWei) {
        this.mAddress = mAddress;
        this.mBalanceInWei = new BigInteger(balanceInWei);
    }

    public String getAddress() {
        return mAddress;
    }

    public BigInteger getBalance() {
        return mBalanceInWei;
    }

    public void setBalance(BigInteger wei) {
        mBalanceInWei = wei;
    }
}
