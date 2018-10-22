package io.stormbird.wallet.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by James on 22/10/2018.
 * Stormbird in Singapore
 */
public class RealmERC721Token extends RealmObject
{
    @PrimaryKey
    private String address;
    private String name;
    private String symbol;
    private long addedTime;
    private long updatedTime;
    private long balanceLength;

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

    public long getBalance() {
        return balanceLength;
    }

    public void setBalanceLength(long balance) {
        this.balanceLength = balance;
    }
}
