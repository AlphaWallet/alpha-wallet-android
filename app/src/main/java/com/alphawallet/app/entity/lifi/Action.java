package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Action
{
    @SerializedName("fromChainId")
    @Expose
    public long fromChainId;

    @SerializedName("toChainId")
    @Expose
    public long toChainId;

    @SerializedName("fromToken")
    @Expose
    public Token fromToken;

    @SerializedName("toToken")
    @Expose
    public Token toToken;

    @SerializedName("fromAmount")
    @Expose
    public String fromAmount;

    @SerializedName("slippage")
    @Expose
    public double slippage;

    @SerializedName("fromAddress")
    @Expose
    public String fromAddress;

    @SerializedName("toAddress")
    @Expose
    public String toAddress;

    public String getCurrentPrice()
    {
        return new BigDecimal(fromToken.priceUSD)
                .divide(new BigDecimal(toToken.priceUSD), 4, RoundingMode.DOWN)
                .stripTrailingZeros()
                .toPlainString();
    }
}