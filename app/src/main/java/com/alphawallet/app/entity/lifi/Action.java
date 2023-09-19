package com.alphawallet.app.entity.lifi;

import android.text.TextUtils;

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
        if (fromToken == null || TextUtils.isEmpty(fromToken.priceUSD) || new BigDecimal(fromToken.priceUSD).equals(BigDecimal.ZERO))
        {
            return "0";
        }
        else
        {
            return new BigDecimal(fromToken.priceUSD)
                    .divide(new BigDecimal(toToken.priceUSD), 4, RoundingMode.DOWN)
                    .stripTrailingZeros()
                    .toPlainString();
        }
    }
}
