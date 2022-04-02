package com.alphawallet.app.entity.opensea;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Stats
{
    @SerializedName("total_supply")
    @Expose
    private long totalSupply;

    @SerializedName("count")
    @Expose
    private long count;

    @SerializedName("num_owners")
    @Expose
    private long numOwners;

    public long getTotalSupply()
    {
        return totalSupply;
    }

    public void setTotalSupply(long totalSupply)
    {
        this.totalSupply = totalSupply;
    }

    public long getCount()
    {
        return count;
    }

    public void setCount(long count)
    {
        this.count = count;
    }

    public long getNumOwners()
    {
        return numOwners;
    }

    public void setNumOwners(long numOwners)
    {
        this.numOwners = numOwners;
    }
}
