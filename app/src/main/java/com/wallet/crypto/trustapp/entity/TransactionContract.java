package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class TransactionContract implements Parcelable {
    public String address;
    public String name;
    public String totalSupply;
    public long decimals;
    public String symbol;

    protected TransactionContract(Parcel in) {
        address = in.readString();
        name = in.readString();
        totalSupply = in.readString();
        decimals = in.readLong();
        symbol = in.readString();
    }

    public static final Creator<TransactionContract> CREATOR = new Creator<TransactionContract>() {
        @Override
        public TransactionContract createFromParcel(Parcel in) {
            return new TransactionContract(in);
        }

        @Override
        public TransactionContract[] newArray(int size) {
            return new TransactionContract[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeString(name);
        parcel.writeString(totalSupply);
        parcel.writeLong(decimals);
        parcel.writeString(symbol);
    }
}
