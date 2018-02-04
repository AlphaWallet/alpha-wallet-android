package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class TokenTicker implements Parcelable {
    public final String id;
    public final String contract;
    public final String price;
    @SerializedName("percent_change_24h")
    public final String percentChange24h;
    public final String image;
    public final String venue;
    public final String date;

    public TokenTicker(String id, String contract, String price, String percentChange24h, String image) {
        this.id = id;
        this.contract = contract;
        this.price = price;
        this.percentChange24h = percentChange24h;
        this.image = image;
        this.venue = null;
        this.date = null;
    }

    public TokenTicker(String id, String contract, String price, String percentChange24h, String image, String venue, String date) {
        this.id = id;
        this.contract = contract;
        this.price = price;
        this.percentChange24h = percentChange24h;
        this.image = image;
        this.venue = venue;
        this.date = date;
    }

    private TokenTicker(Parcel in) {
        id = in.readString();
        contract = in.readString();
        price = in.readString();
        percentChange24h = in.readString();
        image = in.readString();
        byte type = in.readByte();
        switch (type)
        {
            case 0:
                venue = null;
                date = null;
                break;
            case 1:
                venue = in.readString();
                date = in.readString();
                break;
            default:
                venue = null;
                date = null;
                break;
        }
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

        if (venue != null)
        {
            dest.writeByte((byte)1);
            dest.writeString(venue);
            dest.writeString(date);
        }
        else
        {
            dest.writeByte((byte)0);
        }
    }
}
