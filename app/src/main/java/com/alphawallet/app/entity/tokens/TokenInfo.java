package com.alphawallet.app.entity.tokens;

import android.os.Parcel;
import android.os.Parcelable;

public class TokenInfo implements Parcelable {
    public final String address;
    public final String name;
    public final String symbol;
    public final int decimals;
    public final long chainId;
    public boolean isEnabled;

    public TokenInfo(String address, String name, String symbol, int decimals, boolean isEnabled, long chainId) {
        if (address.contains("-"))
        {
            address = address.split("-")[0];
        }
        if (address != null)
        {
            this.address = address.toLowerCase();
        }
        else
        {
            this.address = null;
        }
        this.name = name;
        this.symbol = symbol != null ? symbol.toUpperCase() : null;
        this.decimals = decimals;
        this.isEnabled = isEnabled;
        this.chainId = chainId;
    }

    public TokenInfo()
    {
        address = "";
        name = "";
        symbol = "";
        decimals = 0;
        chainId = 0;
        isEnabled = false;
    }

    public TokenInfo(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        decimals = in.readInt();
        isEnabled = in.readInt() == 1;
        chainId = in.readLong();
    }

    public static final Creator<TokenInfo> CREATOR = new Creator<TokenInfo>() {
        @Override
        public TokenInfo createFromParcel(Parcel in) {
            return new TokenInfo(in);
        }

        @Override
        public TokenInfo[] newArray(int size) {
            return new TokenInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeInt(decimals);
        dest.writeInt(isEnabled ? 1 : 0);
        dest.writeLong(chainId);
    }
}
