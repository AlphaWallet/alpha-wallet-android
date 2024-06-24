package com.langitwallet.app.entity.okx;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.langitwallet.app.entity.OkxEvent;

import java.util.List;

public class OkServiceResponse
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

        @SerializedName("transactionLists")
        @Expose
        public List<OkxEvent> transactionLists;

        @SerializedName("tokenList")
        @Expose
        public List<OkToken> tokenList;
    }
}
