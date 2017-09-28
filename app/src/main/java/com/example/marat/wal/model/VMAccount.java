package com.example.marat.wal.model;

/**
 * Created by marat on 9/26/17.
 */

public class VMAccount {
    private String mAddress;
    private int mBalance;

    public VMAccount(String mAddress, int mBalance) {
        this.mAddress = mAddress;
        this.mBalance = mBalance;
    }

    public String getAddress() {
        return mAddress;
    }

    public int getBalance() {
        return mBalance;
    }
}
