package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class Token implements Parcelable {
    public final TokenInfo tokenInfo;
    public final double balance;

    public Token(TokenInfo tokenInfo, double balance) {
        this.tokenInfo = tokenInfo;
        this.balance = balance;
    }

    private Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = in.readDouble();
    }

    public static final Creator<Token> CREATOR = new Creator<Token>() {
        @Override
        public Token createFromParcel(Parcel in) {
            return new Token(in);
        }

        @Override
        public Token[] newArray(int size) {
            return new Token[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(tokenInfo, flags);
        dest.writeDouble(balance);
    }
}
