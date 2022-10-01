package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SwapProvider
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

    @SerializedName("url")
    @Expose
    public String url;

    public boolean isChecked;
}