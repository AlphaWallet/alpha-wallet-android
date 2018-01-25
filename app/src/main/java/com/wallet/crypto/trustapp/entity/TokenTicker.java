package com.wallet.crypto.trustapp.entity;

import com.google.gson.annotations.SerializedName;

public class TokenTicker {
    public final String id;
    public final String contract;
    public final String price;
    @SerializedName("percent_change_24h")
    public final String percentChange24h;
    public final String image;

    public TokenTicker(String id, String contract, String price, String percentChange24h, String image) {
        this.id = id;
        this.contract = contract;
        this.price = price;
        this.percentChange24h = percentChange24h;
        this.image = image;
    }
}
