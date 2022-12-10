package com.alphawallet.app.entity.unstoppable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class GetRecordsResult
{
    @SerializedName("meta")
    @Expose
    public Meta meta;

    @SerializedName("records")
    @Expose
    public HashMap<String, String> records;
}
