package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Connection
{
    @SerializedName("fromChainId")
    @Expose
    public String fromChainId;

    @SerializedName("toChainId")
    @Expose
    public String toChainId;

    @SerializedName("fromTokens")
    @Expose
    public List<Token> fromTokens;

    @SerializedName("toTokens")
    @Expose
    public List<Token> toTokens;
}
