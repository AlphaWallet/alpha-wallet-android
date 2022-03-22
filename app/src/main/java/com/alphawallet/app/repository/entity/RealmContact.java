package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Chintan on 20/10/2021.
 */
public class RealmContact extends RealmObject
{
    /**
     * This is contact wallet address
     */
    @PrimaryKey
    private String walletAddress;

    /**
     * This is contact name
     */
    private String name;

    /**
     * This is associated ETH name of #walletAddress
     */
    private String ethName;

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEthName() {
        return ethName;
    }

    public void setEthName(String ethName) {
        this.ethName = ethName;
    }
}
