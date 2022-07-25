package com.alphawallet.app.api.v1.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Metadata
{
    @SerializedName("appurl")
    @Expose
    public String appUrl;

    @SerializedName("iconurl")
    @Expose
    public String iconUrl;

    @SerializedName("name")
    @Expose
    public String name;
}
