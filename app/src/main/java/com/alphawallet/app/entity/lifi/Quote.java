package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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
    public SwapProvider swapProvider;

    @SerializedName("action")
    @Expose
    public Action action;

    @SerializedName("estimate")
    @Expose
    public Estimate estimate;

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
}
