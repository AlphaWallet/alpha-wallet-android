package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;

public class Token implements Parcelable {
    public final TokenInfo tokenInfo;
    public final BigDecimal balance;
    public final long updateBlancaTime;

    public TokenTicker ticker;

    public Token(TokenInfo tokenInfo, BigDecimal balance, long updateBlancaTime) {
        this.tokenInfo = tokenInfo;
        this.balance = balance;
        this.updateBlancaTime = updateBlancaTime;
    }

    private Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
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
        dest.writeString(balance.toString());
        dest.writeLong(updateBlancaTime);
    }
}
