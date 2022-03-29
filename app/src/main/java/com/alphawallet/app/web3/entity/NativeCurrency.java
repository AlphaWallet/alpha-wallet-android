package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class NativeCurrency implements Parcelable
{
    public String name;
    public String symbol;
    public int decimals;

    public NativeCurrency(String name, String symbol, int decimals)
    {
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
    }


    protected NativeCurrency(Parcel in)
    {
        name = in.readString();
        symbol = in.readString();
        decimals = in.readInt();
    }

    public static final Creator<NativeCurrency> CREATOR = new Creator<NativeCurrency>()
    {
        @Override
        public NativeCurrency createFromParcel(Parcel in)
        {
            return new NativeCurrency(in);
        }

        @Override
        public NativeCurrency[] newArray(int size)
        {
            return new NativeCurrency[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeInt(decimals);
    }
}
