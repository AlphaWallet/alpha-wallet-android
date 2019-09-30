package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.alphawallet.app.ui.AddTokenActivity;
import com.alphawallet.app.util.Utils;

public class TokenInfo implements Parcelable {
    public final String address;
    public final String name;
    public final String symbol;
    public final int decimals;
    public final int chainId;
    public boolean isEnabled;

    public TokenInfo(String address, String name, String symbol, int decimals, boolean isEnabled, int chainId) {
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

    public TokenInfo(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        decimals = in.readInt();
        isEnabled = in.readInt() == 1;
        chainId = in.readInt();
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
        dest.writeInt(chainId);
    }

    public void addTokenSetupPage(AddTokenActivity layout, String chainName) {
        layout.inputAddressView.setAddress(address);
        layout.symbolInputView.setText(symbol);
        layout.decimalsInputView.setText(String.valueOf(decimals));
        layout.nameInputview.setText(name);
        layout.ticketLayout.setVisibility(View.GONE);

        if (layout.chainName != null)
        {
            layout.chainName.setVisibility(View.VISIBLE);
            layout.chainName.setText(chainName);
            Utils.setChainColour(layout.chainName, chainId);
        }
    }
}
