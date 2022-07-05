package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Chain
{
    @SerializedName("key")
    @Expose
    public String key;

    @SerializedName("name")
    @Expose
    public String name;

    @SerializedName("coin")
    @Expose
    public String coin;

    @SerializedName("id")
    @Expose
    public long id;

    @SerializedName("mainnet")
    @Expose
    public String mainnet;

    @SerializedName("logoURI")
    @Expose
    public String logoURI;

    @SerializedName("tokenlistUrl")
    @Expose
    public String tokenlistUrl;

    @SerializedName("multicallAddress")
    @Expose
    public String multicallAddress;

    @SerializedName("metamask")
    @Expose
    public Metamask metamask;

    public String balance;

    public static class Metamask
    {
        @SerializedName("chainId")
        @Expose
        public String chainId;

        @SerializedName("blockExplorerUrls")
        @Expose
        public List<String> blockExplorerUrls;

        @SerializedName("chainName")
        @Expose
        public String chainName;

        @SerializedName("nativeCurrency")
        @Expose
        public NativeCurrency nativeCurrency;

        @SerializedName("rpcUrls")
        @Expose
        public List<String> rpcUrls;

        public static class NativeCurrency
        {
            @SerializedName("name")
            @Expose
            public String name;

            @SerializedName("symbol")
            @Expose
            public String symbol;

            @SerializedName("decimals")
            @Expose
            public long decimals;
        }
    }
}
