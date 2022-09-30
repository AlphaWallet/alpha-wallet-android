package com.alphawallet.app.entity.opensea;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Rarity
{
    @SerializedName("strategy_id")
    @Expose
    public String strategyId;

    @SerializedName("strategy_version")
    @Expose
    public String strategyVersion;

    @SerializedName("rank")
    @Expose
    public long rank;

    @SerializedName("score")
    @Expose
    public double score;

    @SerializedName("max_rank")
    @Expose
    public long maxRank;

    @SerializedName("tokens_scored")
    @Expose
    public long tokensScored;
}
