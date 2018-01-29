package com.wallet.crypto.trustapp.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.repository.entity.RealmTokenInfo;
import com.wallet.crypto.trustapp.ui.AddTokenActivity;
import com.wallet.crypto.trustapp.ui.widget.holder.TokenHolder;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModel;

import org.web3j.abi.datatypes.generated.Uint16;

import java.math.BigDecimal;
import java.util.List;

public class TokenInfo implements Parcelable, TokenInterface {
    public final String address;
    public final String name;
    public final String symbol;
    public final int decimals;

    public TokenInfo(String address, String name, String symbol, int decimals) {
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
    }

    private TokenInfo(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        decimals = in.readInt();
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
    }

    @Override
    public void setupContent(TokenHolder holder) {
        holder.symbol.setText(this.symbol);
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, decimals));
        BigDecimal ethBalance = decimals > 0
                ? holder.token.balance.divide(decimalDivisor) : holder.token.balance;
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0
                ? "0"
                : ethBalance.toPlainString();
        holder.balance.setText(value);
        holder.balance.setVisibility(View.VISIBLE);
        holder.arrayBalance.setVisibility(View.GONE);
    }

    @Override
    public void addTokenSetupPage(AddTokenActivity layout) {
        layout.address.setText(address);
        layout.symbol.setText(symbol);
        layout.decimals.setText(String.valueOf(decimals));
        layout.name.setText(name);
        layout.ticketLayout.setVisibility(View.GONE);
    }

    @Override
    public void storeRealmData(RealmTokenInfo obj) {
        obj.setName(name);
        obj.setSymbol(symbol);
        obj.setDecimals(decimals);
        obj.setAddedTime(System.currentTimeMillis());
    }

    @Override
    public String populateIDs(List<Uint16> d, boolean keepZeros)
    {
        return "";
    }

    public void clickReact(TokensViewModel viewModel, Context context, int balance)
    {
        viewModel.showSendToken(context, address, symbol, decimals);
    }
}
