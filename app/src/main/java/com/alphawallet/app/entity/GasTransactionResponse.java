package com.alphawallet.app.entity;

import android.util.SparseArray;

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

    private SparseArray<Float> arrayResult;

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

    public SparseArray<Float> getResult() {
        return arrayResult;
    }

    /**
     * Prune duplicate prices off the edges of the range and create sorted minimal array of prices
     */
    public void truncatePriceRange()
    {
        arrayResult = new SparseArray<>();
        SparseArray<Float> priceSet = new SparseArray<>();
        for (String price : result.keySet())
        {
            priceSet.put(Integer.valueOf(price), result.get(price));
        }

        for (int index = priceSet.size() - 1; index > 0; index--)
        {
            int thisPrice = priceSet.keyAt(index);
            int nextPrice = priceSet.keyAt(index-1);
            float thisWaitTime = priceSet.valueAt(index);
            float nextWaitTime = priceSet.valueAt(index-1);
            if (thisWaitTime == nextWaitTime)
            {
                priceSet.delete(Math.max(thisPrice, nextPrice));
            }
        }

        for (int index = 0; index < priceSet.size(); index++)
        {
            if (priceSet.valueAt(index) != null)
            {
                arrayResult.put(priceSet.keyAt(index), priceSet.valueAt(index));
            }
        }
    }
}