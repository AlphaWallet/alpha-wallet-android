package io.stormbird.wallet.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmToken extends RealmObject {
    @PrimaryKey
    private String address;
    private String name;
    private String symbol;
    private int decimals;
    private long addedTime;
    private long updatedTime;
    private String balance;
    private boolean isEnabled;
    private int tokenId;
    private boolean isStormbird;
    private String burnList;
    private int nullCheckCount = 0;

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

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getBurnList()
    {
        return burnList;
    }
    public void setBurnList(String burnList)
    {
        this.burnList = burnList;
    }

    public boolean getEnabled() {
        return isEnabled;
    }

    public boolean isEnabled() { return isEnabled; }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public int getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(int tokenId)
    {
        this.tokenId = tokenId;
    }

    public boolean isStormbird()
    {
        return isStormbird;
    }

    public void setStormbird(boolean stormbird)
    {
        isStormbird = stormbird;
    }

    public int updateNullCheckCount() { return nullCheckCount++; }
    public void setNullCheckCount(int count) { nullCheckCount = count; }

    public int getNullCheckCount()
    {
        return nullCheckCount;
    }
}
