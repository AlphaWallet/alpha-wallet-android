package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Estimate
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

    @SerializedName("feeCosts")
    @Expose
    public ArrayList<FeeCost> feeCosts;

    @SerializedName("gasCosts")
    @Expose
    public ArrayList<GasCost> gasCosts;

    @SerializedName("data")
    @Expose
    public Data data;

    @SerializedName("fromAmountUSD")
    @Expose
    public String fromAmountUSD;

    @SerializedName("toAmountUSD")
    @Expose
    public String toAmountUSD;

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
}