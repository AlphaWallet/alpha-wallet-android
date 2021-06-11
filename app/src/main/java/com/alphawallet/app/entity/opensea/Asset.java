
package com.alphawallet.app.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmERC721Asset;
import com.alphawallet.app.util.Utils;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Asset implements Parcelable {

    private static final String LOADING_TOKEN = "*Loading*";

    @SerializedName("token_id")
    @Expose
    private String tokenId;

    @SerializedName("image_preview_url")
    @Expose
    private String imagePreviewUrl;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("description")
    @Expose
    private String description;

    @SerializedName("external_link")
    @Expose
    private String externalLink;

    @SerializedName("asset_contract")
    @Expose
    private AssetContract assetContract;

    @SerializedName("background_color")
    @Expose
    private String backgroundColor;

    @SerializedName("image_original_url")
    @Expose
    private String imageOriginalUrl;

    @SerializedName("image_thumbnail_url")
    @Expose
    private String imageThumbnailUrl;

    @SerializedName("traits")
    @Expose
    private List<Trait> traits = null;

    public boolean isChecked = false;
    public boolean exposeRadio = false;

    protected Asset(Parcel in) {
        tokenId = in.readString();
        imagePreviewUrl = in.readString();
        imageOriginalUrl = in.readString();
        imageThumbnailUrl = in.readString();
        name = in.readString();
        description = in.readString();
        externalLink = in.readString();
        assetContract = in.readParcelable(AssetContract.class.getClassLoader());
        backgroundColor = in.readString();
        traits = in.createTypedArrayList(Trait.CREATOR);
    }

    public Asset(BigInteger tokenId)
    {
        this.tokenId = tokenId.toString();
        this.name = getGenericName();
        this.description = null;
        this.imagePreviewUrl = null;
        this.imageOriginalUrl = null;
    }

    public Asset(BigInteger tokenId, AssetContract contract)
    {
        this.tokenId = tokenId.toString();
        this.assetContract = contract;
    }

    public static Asset fromMetaData(JSONObject metaData, BigInteger tokenId, Token token)
    {
        AssetContract contract = new AssetContract(token);
        Asset asset = new Asset(tokenId, contract);
        try
        {
            if (metaData.has("name")) asset.name = metaData.getString("name");
            if (metaData.has("image")) asset.imageOriginalUrl = Utils.parseIPFS(metaData.getString("image"));
            else if (metaData.has("image_url")) asset.imageOriginalUrl = Utils.parseIPFS(metaData.getString("image_url"));
            if (metaData.has("description")) asset.description = metaData.getString("description");
            if (metaData.has("external_link")) asset.externalLink = metaData.getString("external_link");
            if (metaData.has("background_color")) asset.backgroundColor = metaData.getString("background_color");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        if (asset.name == null && asset.imageOriginalUrl == null) asset = null;

        return asset;
    }

    public static final Creator<Asset> CREATOR = new Creator<Asset>() {
        @Override
        public Asset createFromParcel(Parcel in) {
            return new Asset(in);
        }

        @Override
        public Asset[] newArray(int size) {
            return new Asset[size];
        }
    };

    public static Asset blankLoading(BigInteger tokenId)
    {
        Asset asset = new Asset(tokenId);
        asset.backgroundColor = LOADING_TOKEN;
        return asset;
    }

    public boolean needsLoading()
    {
        return (!TextUtils.isEmpty(backgroundColor) && backgroundColor.equals(LOADING_TOKEN)
                || TextUtils.isEmpty(imageOriginalUrl));
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getTokenId(int radix) {
        BigInteger bi = new BigInteger(tokenId);
        return bi.toString(radix);
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public Asset withTokenId(String tokenId) {
        this.tokenId = tokenId;
        return this;
    }

    public String getImagePreviewUrl() {
        return imagePreviewUrl;
    }

    public void setImagePreviewUrl(String imagePreviewUrl) {
        this.imagePreviewUrl = imagePreviewUrl;
    }

    public String getImageOriginalUrl() { return imageOriginalUrl; }

    public void setImageOriginalUrl(String imageOriginalUrl) {
        this.imageOriginalUrl = imageOriginalUrl;
    }

    public void setImageThumbnailUrl(String thumbnailUrl) {
        this.imageThumbnailUrl = thumbnailUrl;
    }

    public String getName()
    {
        String assetName;
        if (name != null && !name.equals("null")) {
            assetName = name;
        } else {
            assetName = getGenericName();
        }
        return assetName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Asset withName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        if (TextUtils.isEmpty(description) || description.equals("null")) return "";
        else return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Asset withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public Asset withExternalLink(String externalLink) {
        this.externalLink = externalLink;
        return this;
    }

    public AssetContract getAssetContract() {
        return assetContract;
    }

    public void setAssetContract(AssetContract assetContract) {
        this.assetContract = assetContract;
    }

    public Asset withAssetContract(AssetContract assetContract) {
        this.assetContract = assetContract;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Asset withBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public List<Trait> getTraits() {
        return traits;
    }

    public void setTraits(List<Trait> traits) {
        this.traits = traits;
    }

    public Asset withTraits(List<Trait> traits) {
        this.traits = traits;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tokenId);
        dest.writeString(imagePreviewUrl);
        dest.writeString(imageOriginalUrl);
        dest.writeString(imageThumbnailUrl);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(externalLink);
        dest.writeParcelable(assetContract, flags);
        dest.writeString(backgroundColor);
        dest.writeTypedList(traits);
    }

    public Trait getTraitFromType(String key) {
        if (this.traits != null)
        {
            for (Trait trait : this.traits)
            {
                if (trait.getTraitType() != null && trait.getTraitType().equals(key))
                {
                    return trait;
                }
            }
        }
        return null;
    }

    public boolean hasIdOnly()
    {
        return  !TextUtils.isEmpty(tokenId)
                 && TextUtils.isEmpty(name)
                 && TextUtils.isEmpty(description)
                 && (traits == null || traits.size() == 0);
    }

    public boolean equals(RealmERC721Asset realmAsset)
    {
        List<Trait> traits = realmAsset.getTraits();
        if (traits.size() != realmAsset.getTraits().size() || traitsDifferent(traits, realmAsset.getTraits())) return false;
        else if (realmAsset.getName() == null && name != null
                || !realmAsset.getName().equalsIgnoreCase(name)) return false;
        else if (realmAsset.getImagePreviewUrl() == null && imagePreviewUrl != null
                || (imagePreviewUrl != null && !realmAsset.getImagePreviewUrl().equalsIgnoreCase(imagePreviewUrl))) return false;
        else if (realmAsset.getImageOriginalUrl() == null && imageOriginalUrl != null
                || (imageOriginalUrl != null && !realmAsset.getImageOriginalUrl().equalsIgnoreCase(imageOriginalUrl))) return false;
        else if (realmAsset.getDescription() == null && description != null
                || (description != null && !realmAsset.getDescription().equalsIgnoreCase(description))) return false;
        else return true;
    }

    private boolean traitsDifferent(List<Trait> traits, List<Trait> traits1)
    {
        for (int i = 0; i < traits.size(); i++)
        {
            if (!traits.get(i).getTraitType().equals(traits1.get(i).getTraitType())
                    || !traits.get(i).getValue().equals(traits1.get(i).getValue()))
            {
                return true;
            }
        }

        return false;
    }

    private String getGenericName()
    {
        return "ID# " + tokenId;
    }

    public String getPreviewImageUrl()
    {
        return TextUtils.isEmpty(imagePreviewUrl) ? imageOriginalUrl : imagePreviewUrl;
    }

    public String getBestImageUrl()
    {
        return !TextUtils.isEmpty(imageOriginalUrl) ? imageOriginalUrl : imagePreviewUrl;
    }

    public String getThumbnailUrl()
    {
        return !TextUtils.isEmpty(imageThumbnailUrl) ? imageThumbnailUrl : getPreviewImageUrl();
    }

    public void updateAsset(Map<BigInteger, Asset> oldAssets)
    {
        Asset oldAsset = oldAssets != null ? oldAssets.get(new BigInteger(tokenId)) : null;
        if (oldAsset != null)
        {
            if (TextUtils.isEmpty(name) || name.equals(getGenericName()) && oldAsset.name != null) name = oldAsset.name;
            if (TextUtils.isEmpty(imageOriginalUrl) && oldAsset.imageOriginalUrl != null) imageOriginalUrl = oldAsset.imageOriginalUrl;
            if (TextUtils.isEmpty(imagePreviewUrl) && oldAsset.imagePreviewUrl != null) imagePreviewUrl = oldAsset.imagePreviewUrl;
            if (TextUtils.isEmpty(imageThumbnailUrl) && oldAsset.imageThumbnailUrl != null) imageThumbnailUrl = oldAsset.imageThumbnailUrl;
            if (TextUtils.isEmpty(description) && oldAsset.description != null) description = oldAsset.description;
        }
    }

    public void updateFromRaw(Asset oldAsset)
    {
        imageThumbnailUrl = oldAsset.imageThumbnailUrl;
        imagePreviewUrl = oldAsset.imagePreviewUrl;
        traits = oldAsset.traits;
    }

    public boolean requiresReplacement()
    {
        return (!TextUtils.isEmpty(backgroundColor) && backgroundColor.equals(LOADING_TOKEN) ||
                TextUtils.isEmpty(name) || TextUtils.isEmpty(imagePreviewUrl) ||
                name.equals(getGenericName()));
    }
}
