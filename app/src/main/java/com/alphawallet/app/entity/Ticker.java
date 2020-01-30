package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokens.TokenTicker;
import com.google.gson.annotations.SerializedName;

public class Ticker {
    public String id;
    public String name;
    public String symbol;
    public String price;
    @SerializedName("percent_change_24h")
    public String percentChange24h;
    public String price_usd;

    public Ticker()
    {

    }

    public Ticker(TokenTicker tokenTicker, String sym)
    {
        id = tokenTicker.id;
        name = tokenTicker.contract;
        symbol = sym;
        price = tokenTicker.price;
        percentChange24h = tokenTicker.percentChange24h;
        price_usd = tokenTicker.price;
    }
}
