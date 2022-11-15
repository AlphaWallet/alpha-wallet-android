package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;

public class Wallet implements Parcelable
{
    public final String address;
    public String balance;
    public String ENSname;
    public String name;
    public WalletType type;
    public long lastBackupTime;
    public KeyService.AuthenticationLevel authLevel;
    public long walletCreationTime;
    public String balanceSymbol;
    public String ENSAvatar;
    public boolean isSynced;
    public Token[] tokens;

    public Wallet(String address)
    {
        this.address = address;
        this.balance = "-";
        this.ENSname = "";
        this.name = "";
        this.type = WalletType.NOT_DEFINED;
        this.lastBackupTime = 0;
        this.authLevel = KeyService.AuthenticationLevel.NOT_SET;
        this.walletCreationTime = 0;
        this.balanceSymbol = "";
        this.ENSAvatar = "";
    }

    private Wallet(Parcel in)
    {
        address = in.readString();
        balance = in.readString();
        ENSname = in.readString();
        name = in.readString();
        int t = in.readInt();
        type = WalletType.values()[t];
        lastBackupTime = in.readLong();
        t = in.readInt();
        authLevel = KeyService.AuthenticationLevel.values()[t];
        walletCreationTime = in.readLong();
        balanceSymbol = in.readString();
        ENSAvatar = in.readString();
    }

    public void setWalletType(WalletType wType)
    {
        type = wType;
    }

    public static final Creator<Wallet> CREATOR = new Creator<Wallet>()
    {
        @Override
        public Wallet createFromParcel(Parcel in)
        {
            return new Wallet(in);
        }

        @Override
        public Wallet[] newArray(int size)
        {
            return new Wallet[size];
        }
    };

    public boolean sameAddress(String address)
    {
        return this.address.equalsIgnoreCase(address);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(address);
        parcel.writeString(balance);
        parcel.writeString(ENSname);
        parcel.writeString(name);
        parcel.writeInt(type.ordinal());
        parcel.writeLong(lastBackupTime);
        parcel.writeInt(authLevel.ordinal());
        parcel.writeLong(walletCreationTime);
        parcel.writeString(balanceSymbol);
        parcel.writeString(ENSAvatar);
    }

    public boolean setWalletBalance(Token token)
    {
        balanceSymbol = token.tokenInfo != null ? token.tokenInfo.symbol : "ETH";
        String newBalance = token.getFixedFormattedBalance();
        if (newBalance.equals(balance))
        {
            return false;
        }
        else
        {
            balance = newBalance;
            return true;
        }
    }

    public void zeroWalletBalance(NetworkInfo networkInfo)
    {
        if (balance.equals("-"))
        {
            balanceSymbol = networkInfo.symbol;
            balance = BalanceUtils.getScaledValueFixed(BigDecimal.ZERO, 0, Token.TOKEN_BALANCE_PRECISION);
        }
    }

    public boolean canSign()
    {
        return BuildConfig.DEBUG || !watchOnly();
    }

    public boolean watchOnly()
    {
        return type == WalletType.WATCH;
    }
}
