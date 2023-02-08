package com.alphawallet.app.entity.okx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TokenListReponse
{
    @SerializedName("code")
    @Expose
    public String code;

    @SerializedName("msg")
    @Expose
    public String msg;

    @SerializedName("data")
    @Expose
    public List<Data> data;

    public static class Data
    {
        @SerializedName("page")
        @Expose
        public String page;

        @SerializedName("limit")
        @Expose
        public String limit;

        @SerializedName("totalPage")
        @Expose
        public String totalPage;

        @SerializedName("chainFullName")
        @Expose
        public String chainFullName;

        @SerializedName("chainShortName")
        @Expose
        public String chainShortName;

        @SerializedName("tokenList")
        @Expose
        public List<TokenDetails> tokenList;
    }

    public static class TokenDetails
    {
        @SerializedName("tokenFullName")
        @Expose
        public String tokenFullName;

        @SerializedName("token")
        @Expose
        public String token;

        @SerializedName("precision")
        @Expose
        public String precision;

        @SerializedName("tokenContractAddress")
        @Expose
        public String tokenContractAddress;

        @SerializedName("protocolType")
        @Expose
        public String protocolType;

        @SerializedName("addressCount")
        @Expose
        public String addressCount;

        @SerializedName("totalSupply")
        @Expose
        public String totalSupply;

        @SerializedName("circulatingSupply")
        @Expose
        public String circulatingSupply;

        @SerializedName("price")
        @Expose
        public String price;

        @SerializedName("website")
        @Expose
        public String website;

        @SerializedName("totalMarketCap")
        @Expose
        public String totalMarketCap;
    }
}
