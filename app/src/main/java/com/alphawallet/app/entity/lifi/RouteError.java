package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RouteError
{
    @SerializedName("tool")
    @Expose
    public String tool;

    @SerializedName("message")
    @Expose
    public String message;

    @SerializedName("errorType")
    @Expose
    public String errorType;

    @SerializedName("code")
    @Expose
    public String code;
}
