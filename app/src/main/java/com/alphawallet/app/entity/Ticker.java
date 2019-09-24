package com.alphawallet.app.entity;

import com.google.gson.annotations.SerializedName;

public class Ticker {
    public String id;
    public String name;
    public String symbol;
    public String price;
    @SerializedName("percent_change_24h")
    public String percentChange24h;
    public String price_usd;
}
