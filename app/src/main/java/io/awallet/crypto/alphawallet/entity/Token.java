package io.awallet.crypto.alphawallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.repository.entity.RealmToken;
import io.awallet.crypto.alphawallet.ui.AddTokenActivity;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import io.awallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

import static io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder.EMPTY_BALANCE;

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

    protected Token(Parcel in) {
        tokenInfo = in.readParcelable(TokenInfo.class.getClassLoader());
        balance = new BigDecimal(in.readString());
        updateBlancaTime = in.readLong();
    }

    public String getStringBalance() {
        if (balance != null) return String.valueOf(balance.intValue());
        else return "0";
    }

    public String getFullBalance() {
        if (balance != null) return balance.toString();
        else return "0";
    }

    public String getBurnListStr() {
        return "";
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
    public String getFullName() {
        if (tokenInfo.name == null || tokenInfo.symbol == null) return null;
        else return tokenInfo.name + "(" + tokenInfo.symbol.toUpperCase() + ")";
    }

    public BigInteger getIntAddress() { return Numeric.toBigInt(tokenInfo.address); }

    public void clickReact(BaseViewModel viewModel, Context context)
    {
        viewModel.showSendToken(context, tokenInfo.address, tokenInfo.symbol, tokenInfo.decimals);
    }

    public String populateIDs(List<Integer> d, boolean keepZeros)
    {
        return "";
    }

    public String populateIDs(int[] idArray)
    {
        return "";
    }

    public static final String EMPTY_BALANCE = "\u2014\u2014";

    public void setupContent(TokenHolder holder) {
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? balance.divide(decimalDivisor) : balance;
        ethBalance = ethBalance.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        holder.balanceEth.setText(value);

        if (ticker == null) {
            holder.balanceCurrency.setText(EMPTY_BALANCE);
            holder.fillIcon(null, R.mipmap.token_logo);
            holder.text24Hours.setText(EMPTY_BALANCE);
            holder.textAppreciation.setText(EMPTY_BALANCE);
            holder.textAppreciationSub.setText(R.string.appreciation);
            holder.text24HoursSub.setText(R.string.twenty_four_hours);
        } else {
            holder.textAppreciationSub.setText(R.string.appreciation);
            holder.fillCurrency(ethBalance, ticker);
            holder.fillIcon(ticker.image, R.mipmap.token_logo);
            holder.text24HoursSub.setText(R.string.twenty_four_hours);
    }

        holder.balanceEth.setVisibility(View.VISIBLE);
        holder.arrayBalance.setVisibility(View.GONE);
    }

    public void setRealmBurn(RealmToken realmToken, List<Integer> burnList)
    {

    }

    public List<Integer> parseIndexList(String userList)
    {
        return null;
    }

    public int getTicketCount()
    {
        return balance.intValue();
    }

    public int[] getTicketIndicies(String ticketIds)
    {
        return null;
    }

    public boolean addressMatches(String contractAddress)
    {
        String checkAddress = Numeric.cleanHexPrefix(contractAddress);
        String ourAddress = Numeric.cleanHexPrefix(getAddress());
        if (ourAddress.equalsIgnoreCase(checkAddress))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean isCurrency() {
        return !tokenInfo.isStormbird;
    }

    public List<Integer> indexToIDList(int[] prunedIndices)
    {
        return null;
    }
}