package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GasCost
{
    @SerializedName("amount")
    @Expose
    public String amount;

    @SerializedName("amountUSD")
    @Expose
    public String amountUSD;

    @SerializedName("token")
    @Expose
    public Token token;
}