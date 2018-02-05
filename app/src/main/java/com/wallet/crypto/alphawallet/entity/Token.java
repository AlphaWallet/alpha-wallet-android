package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.wallet.crypto.alphawallet.repository.entity.RealmToken;

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

    protected Token(Parcel in, boolean secondary) {
        updateBlancaTime = in.readLong();
        tokenInfo = null;
        balance = BigDecimal.ZERO;
    }

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
    }

    public String getStringBalance() {
        if (balance != null) return balance.toString();
        else return "0";
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

    public void setRealmBalance(RealmToken realmToken)
    {
        realmToken.setBalance(balance.toString());
    }

    public String getAddress() {
        return tokenInfo.address;
    }
}
