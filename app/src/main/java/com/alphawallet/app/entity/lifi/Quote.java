package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Quote
{
    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("type")
    @Expose
    public String type;

    @SerializedName("tool")
    @Expose
    public String tool;

    @SerializedName("toolDetails")
    @Expose
    public ToolDetails toolDetails;

    public static class ToolDetails
    {
        @SerializedName("key")
        @Expose
        public String key;

        @SerializedName("name")
        @Expose
        public String name;

        @SerializedName("logoURI")
        @Expose
        public String logoURI;
    }

    @SerializedName("action")
    @Expose
    public Action action;

    public static class Action
    {
        @SerializedName("fromChainId")
        @Expose
        public long fromChainId;

        @SerializedName("toChainId")
        @Expose
        public long toChainId;

        @SerializedName("fromToken")
        @Expose
        public Connection.LToken fromToken;

        @SerializedName("toToken")
        @Expose
        public Connection.LToken toToken;

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
    }

    @SerializedName("estimate")
    @Expose
    public Estimate estimate;

    public static class Estimate
    {
        @SerializedName("fromAmount")
        @Expose
        public String fromAmount;

        @SerializedName("toAmount")
        @Expose
        public String toAmount;

        @SerializedName("toAmountMin")
        @Expose
        public String toAmountMin;

        @SerializedName("approvalAddress")
        @Expose
        public String approvalAddress;

        @SerializedName("executionDuration")
        @Expose
        public long executionDuration;

//        @SerializedName("feeCosts")
//        @Expose
//        public JSONArray feeCosts;

        @SerializedName("gasCosts")
        @Expose
        public ArrayList<GasCost> gasCosts;

        public static class GasCost
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

            public static class Token
            {
                @SerializedName("symbol")
                @Expose
                public String symbol;

                @SerializedName("decimals")
                @Expose
                public long decimals;
            }
        }

        @SerializedName("data")
        @Expose
        public Data data;

        public static class Data
        {
            @SerializedName("blockNumber")
            @Expose
            public long blockNumber;

            @SerializedName("network")
            @Expose
            public long network;

            @SerializedName("srcToken")
            @Expose
            public String srcToken;

            @SerializedName("srcDecimals")
            @Expose
            public long srcDecimals;

            @SerializedName("srcAmount")
            @Expose
            public String srcAmount;

            @SerializedName("destToken")
            @Expose
            public String destToken;

            @SerializedName("destDecimals")
            @Expose
            public long destDecimals;

            @SerializedName("destAmount")
            @Expose
            public String destAmount;

            @SerializedName("gasCostUSD")
            @Expose
            public String gasCostUSD;

            @SerializedName("gasCost")
            @Expose
            public String gasCost;

            @SerializedName("buyAmount")
            @Expose
            public String buyAmount;

            @SerializedName("sellAmount")
            @Expose
            public String sellAmount;
        }

        @SerializedName("fromAmountUSD")
        @Expose
        public String fromAmountUSD;

        @SerializedName("toAmountUSD")
        @Expose
        public String toAmountUSD;
    }

    @SerializedName("transactionRequest")
    @Expose
    public TransactionRequest transactionRequest;

    public static class TransactionRequest
    {
        @SerializedName("from")
        @Expose
        public String from;

        @SerializedName("to")
        @Expose
        public String to;

        @SerializedName("chainId")
        @Expose
        public long chainId;

        @SerializedName("data")
        @Expose
        public String data;

        @SerializedName("value")
        @Expose
        public String value;

        @SerializedName("gasLimit")
        @Expose
        public String gasLimit;

        @SerializedName("gasPrice")
        @Expose
        public String gasPrice;
    }

    public String getCurrentPrice()
    {
        return new BigDecimal(action.fromToken.priceUSD)
                .multiply(new BigDecimal(action.toToken.priceUSD)).toString();
    }
}
