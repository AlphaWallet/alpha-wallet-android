package com.wallet.crypto.trustapp.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTokenInfo extends RealmObject {
    @PrimaryKey
    private String address;
    private String name;
    private String symbol;
    private int decimals;
    private long addedTime;

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(long addedTime) {
        this.addedTime = addedTime;
    }
}
