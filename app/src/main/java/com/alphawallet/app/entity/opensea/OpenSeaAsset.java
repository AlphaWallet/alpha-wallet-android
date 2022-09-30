package com.alphawallet.app.entity.opensea;

import android.text.TextUtils;

import com.alphawallet.app.C;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class OpenSeaAsset
{
    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("background_color")
    @Expose
    public String backgroundColor;

    @SerializedName("image")
    @Expose
    public String image;

    @SerializedName("image_url")
    @Expose
    public String imageUrl;

    @SerializedName("image_preview_url")
    @Expose
    public String imagePreviewUrl;

    @SerializedName("image_original_url")
    @Expose
    public String imageOriginalUrl;

    @SerializedName("animationUrl")
    @Expose
    public String animationUrl;

    @SerializedName("animation_url")
    @Expose
    public String animation_url;

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

    @SerializedName("rarity_data")
    @Expose
    public Rarity rarity;

    public static class Collection
    {
        @SerializedName("stats")
        @Expose
        public Stats stats;

        @SerializedName("banner_image_url")
        @Expose
        public String bannerImageUrl;

        @SerializedName("slug")
        @Expose
        public String slug;

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

            @SerializedName("average_price")
            @Expose
            public String averagePrice;

            @SerializedName("floor_price")
            @Expose
            public String floorPrice;
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
            traitRarity = ((float) traitCount * 100) / totalSupply;
            return traitRarity;
        }

        public void updateTraitRarity(long totalSupply)
        {
            traitRarity = ((float) traitCount * 100) / totalSupply;
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

    public String getAveragePrice()
    {
        if (collection != null && collection.stats != null)
        {
            String avgPrice = collection.stats.averagePrice;
            if (!TextUtils.isEmpty(avgPrice))
            {
                BigDecimal p = new BigDecimal(collection.stats.averagePrice);
                return p.setScale(3, RoundingMode.CEILING) + " " + C.ETH_SYMBOL;
                // This method is only called for mainnet queries, hence the hardcoded ETH Symbol
            }
        }
        return "";
    }

    public String getFloorPrice()
    {
        if (collection != null && collection.stats != null)
        {
            String floorPrice = collection.stats.floorPrice;
            if (!TextUtils.isEmpty(floorPrice))
            {
                return floorPrice + " " + C.ETH_SYMBOL;
                // This method is only called for mainnet queries, hence the hardcoded ETH Symbol
            }
        }
        return "";
    }

    public String getLastSale()
    {
        if (lastSale != null && lastSale.paymentToken != null)
        {
            StringBuilder res = computeLastSale(lastSale.totalPrice, lastSale.paymentToken.decimals);
            if (!TextUtils.isEmpty(res))
            {
                return res.append(" ").append(lastSale.paymentToken.symbol).toString();
            }
        }
        return "";
    }

    private StringBuilder computeLastSale(String totalPrice, int decimals)
    {
        StringBuilder result = new StringBuilder();
        if (totalPrice.equals("0"))
        {
            return result;
        }
        else if (totalPrice.length() <= decimals)
        {
            result.append("0.");
            int diff = decimals - totalPrice.length();
            for (int i = 0; i < diff; i++)
            {
                result.append("0");
            }

            for (int i = 0; i < totalPrice.length(); i++)
            {
                char c = totalPrice.charAt(i);
                if (c == '0')
                {
                    break;
                }
                else
                {
                    result.append(c);
                }
            }
        }
        else
        {
            int endIndex = totalPrice.length() - decimals;
            result.append(totalPrice.substring(0, endIndex));
            result.append(".");

            for (int i = 0; i < totalPrice.length(); i++)
            {
                char c = totalPrice.charAt(i);
                if (c == '0')
                {
                    result.append(c);
                    break;
                }
                else
                {
                    result.append(c);
                }
            }
        }
        return result;
    }

    public String getAnimationUrl()
    {
        if (animationUrl != null)
        {
            return animationUrl;
        }
        else if (animation_url != null)
        {
            return animation_url;
        }
        else
        {
            return null;
        }
    }

    public String getImageUrl()
    {
        if (image != null)
        {
            return image;
        }
        else if (imageUrl != null)
        {
            return imageUrl;
        }
        else if (animationUrl != null)
        {
            return animationUrl;
        }
        else if (imageOriginalUrl != null)
        {
            return imageOriginalUrl;
        }
        else if (imagePreviewUrl != null)
        {
            return imagePreviewUrl;
        }
        else if (animation_url != null)
        {
            return animation_url;
        }
        else
        {
            return "";
        }
    }

    public boolean isValid()
    {
        return !TextUtils.isEmpty(getImageUrl())
                || !TextUtils.isEmpty(name)
                || !TextUtils.isEmpty(description);
    }
}
