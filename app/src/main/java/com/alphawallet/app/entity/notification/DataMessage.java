package com.alphawallet.app.entity.notification;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DataMessage
{
    @SerializedName("title")
    @Expose
    public Title title;

    @SerializedName("body")
    @Expose
    public Body body;

    public static class Title
    {
        @SerializedName("contract")
        @Expose
        public String contract;

        @SerializedName("wallet")
        @Expose
        public String wallet;

        @SerializedName("createdAt")
        @Expose
        public String createdAt;

        @SerializedName("chainId")
        @Expose
        public String chainId;

        @SerializedName("event")
        @Expose
        public String event;
    }

    public static class Body
    {
        @SerializedName("to")
        @Expose
        public String to;

        @SerializedName("from")
        @Expose
        public String from;

        @SerializedName("chain")
        @Expose
        public String chain;

        @SerializedName("event")
        @Expose
        public String event;

        @SerializedName("contract")
        @Expose
        public String contract;

        @SerializedName("blockNumber")
        @Expose
        public String blockNumber;

        @SerializedName("contractType")
        @Expose
        public String contractType;
    }
}
