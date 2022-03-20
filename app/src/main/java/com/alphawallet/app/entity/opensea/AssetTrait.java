package com.alphawallet.app.entity.opensea;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AssetTrait {
    @SerializedName("trait_type")
    @Expose
    private String traitType;

    @SerializedName("value")
    @Expose
    private String value;

    @SerializedName("trait_count")
    @Expose
    private long traitCount;

    private float traitRarity;

    public float getTraitRarity()
    {
        return traitRarity;
    }

    public void setTraitRarity(float traitRarity)
    {
        this.traitRarity = traitRarity;
    }

    public long getTraitCount()
    {
        return traitCount;
    }

    public void setTraitCount(long traitCount)
    {
        this.traitCount = traitCount;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getTraitType()
    {
        return traitType;
    }

    public void setTraitType(String traitType)
    {
        this.traitType = traitType;
    }
}
