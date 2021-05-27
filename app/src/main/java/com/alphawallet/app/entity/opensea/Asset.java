
package com.alphawallet.app.entity.opensea;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmERC721Asset;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
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

    private String metadataImageUrl;

    public boolean isChecked = false;
    public boolean exposeRadio = false;

    protected Asset(Parcel in) {
        tokenId = in.readString();
        imagePreviewUrl = in.readString();
        metadataImageUrl = in.readString();
        name = in.readString();
        description = in.readString();
        externalLink = in.readString();
        assetContract = in.readParcelable(AssetContract.class.getClassLoader());
        backgroundColor = in.readString();
        traits = in.createTypedArrayList(Trait.CREATOR);
    }

    public Asset()
    {
        tokenId = null;
    }

    public Asset(String tokenId, AssetContract contract)
    {
        if (Utils.isHex(tokenId))
        {
            //tokenId in hex - convert to decimal
            BigInteger bi = new BigInteger(tokenId, 16);
            tokenId = bi.toString(10);
        }
        this.tokenId = tokenId;
        this.assetContract = contract;
    }

    public static Asset blankFromToken(Token token, String tokenId)
    {
        AssetContract contract = new AssetContract(token);
        Asset asset = new Asset(tokenId, contract);
        asset.name = "";
        asset.description = "";
        asset.imagePreviewUrl = "";
        return asset;
    }

    public static Asset fromMetaData(JSONObject metaData, String tokenId, Token token)
    {
        AssetContract contract = new AssetContract(token);
        Asset asset = new Asset(tokenId, contract);
        try
        {
            if (metaData.has("name")) asset.name = metaData.getString("name");
            if (metaData.has("image")) asset.metadataImageUrl = Utils.parseIPFS(metaData.getString("image"));
            if (metaData.has("description")) asset.description = metaData.getString("description");
            if (metaData.has("external_link")) asset.externalLink = metaData.getString("external_link");
            if (metaData.has("background_color")) asset.backgroundColor = metaData.getString("background_color");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        if (asset.name == null && asset.metadataImageUrl == null) asset = null;

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

    public String getImageOriginalUrl() { return metadataImageUrl; }

    public void setImageOriginalUrl(String imageOriginalUrl) {
        this.metadataImageUrl = imageOriginalUrl;
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
        dest.writeString(metadataImageUrl);
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
        else if (realmAsset.getImagePreviewUrl() == null && imagePreviewUrl != null
                || realmAsset.getImagePreviewUrl().equalsIgnoreCase(imagePreviewUrl)) return false;
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

    public void loadNFTImage(Context context, RequestListener<Drawable> requestListener, ImageView image)
    {
        String url = imagePreviewUrl != null ? imagePreviewUrl : metadataImageUrl;
        Glide.with(context)
                .load(url)
                .listener(requestListener)
                .into(image);
    }
}
