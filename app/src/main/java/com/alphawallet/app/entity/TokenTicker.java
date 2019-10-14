package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class TokenTicker implements Parcelable {
    public final String id;
    public final String contract;
    public final String price;
    public final String priceSymbol;
    @SerializedName("percent_change_24h")
    public final String percentChange24h;
    public final String image;

    public TokenTicker(String id, String contract, String price, String percentChange24h, String symbol, String image) {
        this.id = id;
        this.contract = contract;
        this.price = price;
        this.percentChange24h = percentChange24h;
        this.image = image;
        this.priceSymbol = symbol;
    }

    public TokenTicker(Ticker ticker, String contract, String image) {
        this.id = ticker.id;
        this.contract = contract;
        if (ticker.price != null)
        {
            this.price = ticker.price;
        }
        else
        {
            this.price = ticker.price_usd;
        }
        this.percentChange24h = ticker.percentChange24h;
        this.image = image;
        if (ticker.symbol == null)
        {
            this.priceSymbol = "USD";
        }
        else
        {
            this.priceSymbol = ticker.symbol;
        }
    }

    private TokenTicker(Parcel in) {
        id = in.readString();
        contract = in.readString();
        price = in.readString();
        percentChange24h = in.readString();
        image = in.readString();
        priceSymbol = in.readString();
    }

    public static final Creator<TokenTicker> CREATOR = new Creator<TokenTicker>() {
        @Override
        public TokenTicker createFromParcel(Parcel in) {
            return new TokenTicker(in);
        }

        @Override
        public TokenTicker[] newArray(int size) {
            return new TokenTicker[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(contract);
        dest.writeString(price);
        dest.writeString(percentChange24h);
        dest.writeString(image);
        dest.writeString(priceSymbol);
    }
}
