package com.alphawallet.app.entity.opensea;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OpenSeaAsset
{
    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("image_url")
    @Expose
    public String imageUrl;

    @SerializedName("name")
    @Expose
    public String name;

    @SerializedName("description")
    @Expose
    public String description;

    @SerializedName("external_link")
    @Expose
    public String externalLink;

    @SerializedName("asset_contract")
    @Expose
    public AssetContract assetContract;

    @SerializedName("permalink")
    @Expose
    public String permalink;

    @SerializedName("collection")
    @Expose
    public Collection collection;

    @SerializedName("token_metadata")
    @Expose
    public String tokenMetadata;

    @SerializedName("owner")
    @Expose
    public Owner owner;

    @SerializedName("creator")
    @Expose
    public Creator creator;

    @SerializedName("traits")
    @Expose
    public List<Trait> traits;

    @SerializedName("last_sale")
    @Expose
    public LastSale lastSale;

    public static class Collection
    {
        @SerializedName("stats")
        @Expose
        public Stats stats;

        @SerializedName("banner_image_url")
        @Expose
        public String bannerImageUrl;

        public static class Stats
        {
            @SerializedName("total_supply")
            @Expose
            public long totalSupply;

            @SerializedName("count")
            @Expose
            public long count;

            @SerializedName("num_owners")
            @Expose
            public long numOwners;
        }
    }

    public static class Owner
    {
        @SerializedName("user")
        @Expose
        public User user;

        @SerializedName("profile_img_url")
        @Expose
        public String profileImgUrl;

        @SerializedName("address")
        @Expose
        public String address;

        public static class User
        {
            @SerializedName("username")
            @Expose
            public String username;
        }
    }

    public static class Creator
    {
        @SerializedName("user")
        @Expose
        public User user;

        public static class User
        {
            @SerializedName("username")
            @Expose
            public String username;
        }
    }

    public static class Trait
    {
        @SerializedName("trait_type")
        @Expose
        public String traitType;

        @SerializedName("value")
        @Expose
        public String value;

        @SerializedName("trait_count")
        @Expose
        public long traitCount;

        public float traitRarity;

        public boolean isUnique;

        public Trait(String key, String attrVal)
        {
            traitType = key;
            value = attrVal;
            traitCount = 0;
            isUnique = false;
        }

        public boolean isUnique()
        {
            return this.isUnique;
        }

        public void setUnique(boolean isUnique)
        {
            this.isUnique = isUnique;
        }

        public float getTraitRarity(long totalSupply)
        {
            setUnique(traitCount == 1);
            traitRarity =  ((float) traitCount * 100) / totalSupply;
            return traitRarity;
        }

        public void updateTraitRarity(long totalSupply)
        {
            traitRarity =  ((float) traitCount * 100) / totalSupply;
        }
    }

    public static class LastSale
    {
        @SerializedName("total_price")
        @Expose
        public String totalPrice;

        @SerializedName("payment_token")
        @Expose
        public PaymentToken paymentToken;

        public static class PaymentToken
        {
            @SerializedName("symbol")
            @Expose
            public String symbol;

            @SerializedName("decimals")
            @Expose
            public int decimals;
        }
    }
}
