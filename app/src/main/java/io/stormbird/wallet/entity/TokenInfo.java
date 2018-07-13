package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import io.stormbird.wallet.ui.AddTokenActivity;

public class TokenInfo implements Parcelable {
    public final String address;
    public final String name;
    public final String symbol;
    public final int decimals;
    public boolean isEnabled;
    public final boolean isStormbird;

    public TokenInfo(String address, String name, String symbol, int decimals, boolean isEnabled) {
        if (address != null)
        {
            this.address = address.toLowerCase();
        }
        else
        {
            this.address = null;
        }
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.isEnabled = isEnabled;
        this.isStormbird = false;
    }
    public TokenInfo(String address, String name, String symbol, int decimals, boolean isEnabled, boolean isStormbird) {
        if (address != null)
        {
            this.address = address.toLowerCase();
        }
        else
        {
            this.address = null;
        }
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.isEnabled = isEnabled;
        this.isStormbird = isStormbird;
    }

    public TokenInfo(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        decimals = in.readInt();
        isEnabled = in.readInt() == 1;
        isStormbird = in.readInt() == 1;
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
        dest.writeInt(isStormbird ? 1 : 0);
    }

    public void addTokenSetupPage(AddTokenActivity layout) {
        layout.inputAddressView.setAddress(address);
        layout.symbolInputView.setText(symbol);
        layout.decimalsInputView.setText(String.valueOf(decimals));
        layout.nameInputview.setText(name);
        layout.ticketLayout.setVisibility(View.GONE);
        layout.isStormbird = isStormbird;
    }
}
