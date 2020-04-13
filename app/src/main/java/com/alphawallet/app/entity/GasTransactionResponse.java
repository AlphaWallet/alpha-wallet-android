package com.alphawallet.app.entity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class GasTransactionResponse {

    @SerializedName("fast")
    @Expose
    private Float fast;

    @SerializedName("fastest")
    @Expose
    private Float fastest;

    @SerializedName("safeLow")
    @Expose
    private Float safeLow;

    @SerializedName("average")
    @Expose
    private Float average;

    @SerializedName("block_time")
    @Expose
    private Float blockTime;

    @SerializedName("blockNum")
    @Expose
    private Float blockNum;

    @SerializedName("speed")
    @Expose
    private Float speed;

    @SerializedName("safeLowWait")
    @Expose
    private Float safeLowWait;

    @SerializedName("avgWait")
    @Expose
    private Float avgWait;

    @SerializedName("fastWait")
    @Expose
    private Float fastWait;

    @SerializedName("fastestWait")
    @Expose
    private Float fastestWait;

    @SerializedName("gasPriceRange")
    @Expose
    private Map<String, Float> result;

    public Float getFast() {
        return fast;
    }

    public Float getFastest() {
        return fastest;
    }

    public Float getSafeLow() {
        return safeLow;
    }

    public Float getAverage() {
        return average;
    }

    public Float getBlockTime() {
        return blockTime;
    }

    public Float getBlockNum() {
        return blockNum;
    }

    public Float getSpeed() {
        return speed;
    }

    public Float getSafeLowWait() {
        return safeLowWait;
    }

    public Float getAvgWait() {
        return avgWait;
    }

    public Float getFastWait() {
        return fastWait;
    }

    public Float getFastestWait() {
        return fastestWait;
    }

    public Map<String, Float> getResult() {
        return result;
    }
}