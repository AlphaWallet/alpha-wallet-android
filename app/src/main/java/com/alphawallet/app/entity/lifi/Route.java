package com.alphawallet.app.entity.lifi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Route
{
    @SerializedName("gasCostUSD")
    @Expose
    public String gasCostUSD;

    @SerializedName("steps")
    @Expose
    public List<Step> steps;

    @SerializedName("tags")
    @Expose
    public List<String> tags;

    public class Step
    {
        @SerializedName("toolDetails")
        @Expose
        public ToolDetails toolDetails;

        @SerializedName("action")
        @Expose
        public Action action;

        @SerializedName("estimate")
        @Expose
        public Estimate estimate;

        @Override
        public String toString()
        {
            return "Step{" +
                    "toolDetails=" + toolDetails +
                    ", estimate=" + estimate.toString() +
                    '}';
        }
    }

    @Override
    public String toString()
    {
        return "Route{" +
                "gasCostUSD='" + gasCostUSD + '\'' +
                ", steps=" + steps.toString() +
                '}';
    }
}
