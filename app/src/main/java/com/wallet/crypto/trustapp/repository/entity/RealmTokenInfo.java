package com.wallet.crypto.trustapp.repository.entity;

import java.math.BigDecimal;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmTokenInfo extends RealmObject {
    @PrimaryKey
    private String address;
    private String name;
    private String symbol;
    private int decimals;
    private long addedTime;
    private boolean isDisable;

    //So far Realm doesn't support extending RealmTokenInfo as a base class :(
    private String venue = null;
    private String date = null;
    private double price = 0;

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

    public boolean isDisable() {
        return isDisable;
    }

    public void setDisable(boolean disable) {
        isDisable = disable;
    }

    public String getVenue()
    {
        return venue;
    }

    public void setVenue(String venue)
    {
        this.venue = venue;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public double getPrice()
    {
        return price;
    }

    public void setPrice(double price)
    {
        this.price = price;
    }
}
