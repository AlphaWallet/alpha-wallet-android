
package io.stormbird.wallet.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Asset implements Parcelable {

    @SerializedName("token_id")
    @Expose
    private String tokenId;
    @SerializedName("image_url")
    @Expose
    private String imageUrl;
    @SerializedName("image_preview_url")
    @Expose
    private String imagePreviewUrl;
    @SerializedName("image_thumbnail_url")
    @Expose
    private String imageThumbnailUrl;
    @SerializedName("image_original_url")
    @Expose
    private String imageOriginalUrl;
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
    @SerializedName("owner")
    @Expose
    private Owner owner;
    @SerializedName("permalink")
    @Expose
    private String permalink;
    @SerializedName("background_color")
    @Expose
    private String backgroundColor;
    @SerializedName("auctions")
    @Expose
    private Object auctions;
    @SerializedName("sell_orders")
    @Expose
    private List<Object> sellOrders = null;
    @SerializedName("traits")
    @Expose
    private List<Trait> traits = null;
    @SerializedName("last_sale")
    @Expose
    private Object lastSale;
    @SerializedName("num_sales")
    @Expose
    private long numSales;
    @SerializedName("top_bid")
    @Expose
    private Object topBid;
    @SerializedName("current_price")
    @Expose
    private Object currentPrice;
    @SerializedName("current_escrow_price")
    @Expose
    private Object currentEscrowPrice;
    @SerializedName("listing_date")
    @Expose
    private Object listingDate;
    @SerializedName("is_presale")
    @Expose
    private boolean isPresale;

    protected Asset(Parcel in) {
        tokenId = in.readString();
        imageUrl = in.readString();
        imagePreviewUrl = in.readString();
        imageThumbnailUrl = in.readString();
        imageOriginalUrl = in.readString();
        name = in.readString();
        description = in.readString();
        externalLink = in.readString();
        assetContract = in.readParcelable(AssetContract.class.getClassLoader());
        owner = in.readParcelable(Owner.class.getClassLoader());
        permalink = in.readString();
        backgroundColor = in.readString();
        traits = in.createTypedArrayList(Trait.CREATOR);
        numSales = in.readLong();
        isPresale = in.readByte() != 0;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Asset withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getImageThumbnailUrl() {
        return imageThumbnailUrl;
    }

    public void setImageThumbnailUrl(String imageThumbnailUrl) {
        this.imageThumbnailUrl = imageThumbnailUrl;
    }

    public Asset withImageThumbnailUrl(String imageThumbnailUrl) {
        this.imageThumbnailUrl = imageThumbnailUrl;
        return this;
    }

    public String getImageOriginalUrl() {
        return imageOriginalUrl;
    }

    public void setImageOriginalUrl(String imageOriginalUrl) {
        this.imageOriginalUrl = imageOriginalUrl;
    }

    public Asset withImageOriginalUrl(String imageOriginalUrl) {
        this.imageOriginalUrl = imageOriginalUrl;
        return this;
    }

    public String getName() {
        return name;
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

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Asset withOwner(Owner owner) {
        this.owner = owner;
        return this;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public Asset withPermalink(String permalink) {
        this.permalink = permalink;
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

    public Object getAuctions() {
        return auctions;
    }

    public void setAuctions(Object auctions) {
        this.auctions = auctions;
    }

    public Asset withAuctions(Object auctions) {
        this.auctions = auctions;
        return this;
    }

    public List<Object> getSellOrders() {
        return sellOrders;
    }

    public void setSellOrders(List<Object> sellOrders) {
        this.sellOrders = sellOrders;
    }

    public Asset withSellOrders(List<Object> sellOrders) {
        this.sellOrders = sellOrders;
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

    public Object getLastSale() {
        return lastSale;
    }

    public void setLastSale(Object lastSale) {
        this.lastSale = lastSale;
    }

    public Asset withLastSale(Object lastSale) {
        this.lastSale = lastSale;
        return this;
    }

    public long getNumSales() {
        return numSales;
    }

    public void setNumSales(long numSales) {
        this.numSales = numSales;
    }

    public Asset withNumSales(long numSales) {
        this.numSales = numSales;
        return this;
    }

    public Object getTopBid() {
        return topBid;
    }

    public void setTopBid(Object topBid) {
        this.topBid = topBid;
    }

    public Asset withTopBid(Object topBid) {
        this.topBid = topBid;
        return this;
    }

    public Object getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Object currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Asset withCurrentPrice(Object currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    public Object getCurrentEscrowPrice() {
        return currentEscrowPrice;
    }

    public void setCurrentEscrowPrice(Object currentEscrowPrice) {
        this.currentEscrowPrice = currentEscrowPrice;
    }

    public Asset withCurrentEscrowPrice(Object currentEscrowPrice) {
        this.currentEscrowPrice = currentEscrowPrice;
        return this;
    }

    public Object getListingDate() {
        return listingDate;
    }

    public void setListingDate(Object listingDate) {
        this.listingDate = listingDate;
    }

    public Asset withListingDate(Object listingDate) {
        this.listingDate = listingDate;
        return this;
    }

    public boolean isIsPresale() {
        return isPresale;
    }

    public void setIsPresale(boolean isPresale) {
        this.isPresale = isPresale;
    }

    public Asset withIsPresale(boolean isPresale) {
        this.isPresale = isPresale;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tokenId);
        dest.writeString(imageUrl);
        dest.writeString(imagePreviewUrl);
        dest.writeString(imageThumbnailUrl);
        dest.writeString(imageOriginalUrl);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(externalLink);
        dest.writeParcelable(assetContract, flags);
        dest.writeParcelable(owner, flags);
        dest.writeString(permalink);
        dest.writeString(backgroundColor);
        dest.writeTypedList(traits);
        dest.writeLong(numSales);
        dest.writeByte((byte) (isPresale ? 1 : 0));
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
