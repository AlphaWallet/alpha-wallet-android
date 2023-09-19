package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class FeeCost
{
    @SerializedName("name")
    @Expose
    public String name;

    @SerializedName("percentage")
    @Expose
    public String percentage;

    @SerializedName("token")
    @Expose
    public Token token;

    @SerializedName("amount")
    @Expose
    public String amount;
}