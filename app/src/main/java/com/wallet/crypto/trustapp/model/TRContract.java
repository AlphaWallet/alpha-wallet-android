package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by msubkhankulov on 12/16/2017.
 */

/* Based on the following JSON
{
    "_id": "5a055b617946851ff3f85327",
    "address": "0xab95e915c123fded5bdfb6325e35ef5515f1ea69",
    "name": "XENON",
    "totalSupply": "1000000000000000000000000000",
    "decimals": 18,
    "symbol": "XNN"
}
 */

public class TRContract {

    @SerializedName("address")
    private String address;

    @SerializedName("name")
    private String name;

    @SerializedName("totalSupply")
    private String totalSupply;

    @SerializedName("decimals")
    private String decimals;

    @SerializedName("symbol")
    private String symbol;

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public String getTotalSupply() {
        return totalSupply;
    }

    public String getDecimals() {
        return decimals;
    }

    public String getSymbol() {
        return symbol;
    }
}
