package com.wallet.crypto.trustapp.entity;

import com.google.gson.annotations.SerializedName;

public class TokenTicker {
    public final String contract;
    public final String price;
    @SerializedName("percent_change_24h")
    public final String percentChange24h;

    public TokenTicker(String contract, String price, String percentChange24h) {
        this.contract = contract;
        this.price = price;
        this.percentChange24h = percentChange24h;
    }
}
