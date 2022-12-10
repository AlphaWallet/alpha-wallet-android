package com.alphawallet.app.entity.unstoppable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Meta
{
    @SerializedName("resolver")
    @Expose
    public String resolver;

    @SerializedName("blockchain")
    @Expose
    public String blockchain;

    @SerializedName("networkId")
    @Expose
    public long networkId;

    @SerializedName("registry")
    @Expose
    public String registry;

    @SerializedName("domain")
    @Expose
    public String domain;

    @SerializedName("owner")
    @Expose
    public String owner;

    @SerializedName("reverse")
    @Expose
    public boolean reverse;
}
