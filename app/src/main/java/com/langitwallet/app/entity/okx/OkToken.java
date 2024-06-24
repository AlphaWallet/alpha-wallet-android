package com.langitwallet.app.entity.okx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.langitwallet.app.entity.tokens.TokenInfo;

public class OkToken
{
    @SerializedName("symbol")
    @Expose
    public String symbol;

    @SerializedName("tokenContractAddress")
    @Expose
    public String tokenContractAddress;

    @SerializedName("holdingAmount")
    @Expose
    public String holdingAmount;

    @SerializedName("priceUsd")
    @Expose
    public String priceUsd;

    @SerializedName("tokenId")
    @Expose
    public String tokenId;

    public TokenInfo createInfo(long chainId)
    {
        return new TokenInfo(tokenContractAddress, "", symbol, 0, true, chainId);
    }
}
