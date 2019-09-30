
package com.alphawallet.app.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Asset implements Parcelable {

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

    @SerializedName("traits")
    @Expose
    private List<Trait> traits = null;

    public boolean isChecked = false;
    public boolean exposeRadio = false;

    protected Asset(Parcel in) {
        tokenId = in.readString();
        imagePreviewUrl = in.readString();
        name = in.readString();
        description = in.readString();
        externalLink = in.readString();
        assetContract = in.readParcelable(AssetContract.class.getClassLoader());
        backgroundColor = in.readString();
        traits = in.createTypedArrayList(Trait.CREATOR);
    }

    public Asset(String tokenId, AssetContract contract)
    {
        this.tokenId = tokenId;
        this.assetContract = contract;
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

    public String getTokenId() {
        return tokenId;
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

    public Asset withImagePreviewUrl(String imagePreviewUrl) {
        this.imagePreviewUrl = imagePreviewUrl;
        return this;
    }

    public String getName()
    {
        String assetName;
        if (name != null && !name.equals("null")) {
            assetName = name;
        } else {
            assetName = "ID# " + String.valueOf(tokenId);
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
        return description;
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
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(externalLink);
        dest.writeParcelable(assetContract, flags);
        dest.writeString(backgroundColor);
        dest.writeTypedList(traits);
    }

    public Trait getTraitFromType(String key) {
        for (Trait trait : this.traits) {
            if (trait.getTraitType() != null && trait.getTraitType().equals(key)) {
                return trait;
            }
        }
        return null;
    }
}
