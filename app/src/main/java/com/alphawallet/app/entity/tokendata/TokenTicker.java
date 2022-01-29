package com.alphawallet.app.entity.tokendata;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.google.gson.annotations.SerializedName;

public class TokenTicker implements Parcelable {
    public final String price;
    public final String priceSymbol;
    @SerializedName("percent_change_24h")
    public final String percentChange24h;
    public final String image;
    public final long updateTime;

    public TokenTicker()
    {
        price = "0";
        percentChange24h = "0.0";
        image = "";
        priceSymbol = "USD";
        updateTime = 0;
    }

    public TokenTicker(long uTime) //blank
    {
        price = "";
        percentChange24h = "";
        image = "";
        priceSymbol = "";
        updateTime = uTime;
    }

    public TokenTicker(String price, String percentChange24h, String priceSymbol, String image, long updateTime) {
        this.price = price;
        this.percentChange24h = percentChange24h;
        this.image = image;
        this.priceSymbol = priceSymbol;
        this.updateTime = updateTime;
    }

    private TokenTicker(Parcel in) {
        price = in.readString();
        percentChange24h = in.readString();
        image = in.readString();
        priceSymbol = in.readString();
        updateTime = in.readLong();
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
        dest.writeString(price);
        dest.writeString(percentChange24h);
        dest.writeString(image);
        dest.writeString(priceSymbol);
        dest.writeLong(updateTime);
    }
}
