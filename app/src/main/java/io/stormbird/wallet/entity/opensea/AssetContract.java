
package io.stormbird.wallet.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AssetContract implements Parcelable {

    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("symbol")
    @Expose
    private String symbol;
    @SerializedName("image_url")
    @Expose
    private String imageUrl;
    @SerializedName("featured_image_url")
    @Expose
    private String featuredImageUrl;
    @SerializedName("featured")
    @Expose
    private boolean featured;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("external_link")
    @Expose
    private String externalLink;
    @SerializedName("wiki_link")
    @Expose
    private Object wikiLink;
    @SerializedName("stats")
    @Expose
    private Object stats;
    @SerializedName("traits")
    @Expose
    private Object traits;
    @SerializedName("hidden")
    @Expose
    private boolean hidden;
    @SerializedName("nft_version")
    @Expose
    private String nftVersion;
    @SerializedName("schema_name")
    @Expose
    private String schemaName;
    @SerializedName("display_data")
    @Expose
    private DisplayData displayData;
    @SerializedName("short_description")
    @Expose
    private String shortDescription;
    @SerializedName("total_supply")
    @Expose
    private Object totalSupply;
    @SerializedName("buyer_fee_basis_points")
    @Expose
    private long buyerFeeBasisPoints;
    @SerializedName("seller_fee_basis_points")
    @Expose
    private long sellerFeeBasisPoints;

    protected AssetContract(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        imageUrl = in.readString();
        featuredImageUrl = in.readString();
        featured = in.readByte() != 0;
        description = in.readString();
        externalLink = in.readString();
        hidden = in.readByte() != 0;
        nftVersion = in.readString();
        schemaName = in.readString();
        shortDescription = in.readString();
        buyerFeeBasisPoints = in.readLong();
        sellerFeeBasisPoints = in.readLong();
    }

    public static final Creator<AssetContract> CREATOR = new Creator<AssetContract>() {
        @Override
        public AssetContract createFromParcel(Parcel in) {
            return new AssetContract(in);
        }

        @Override
        public AssetContract[] newArray(int size) {
            return new AssetContract[size];
        }
    };

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public AssetContract withAddress(String address) {
        this.address = address;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AssetContract withName(String name) {
        this.name = name;
        return this;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public AssetContract withSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public AssetContract withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public String getFeaturedImageUrl() {
        return featuredImageUrl;
    }

    public void setFeaturedImageUrl(String featuredImageUrl) {
        this.featuredImageUrl = featuredImageUrl;
    }

    public AssetContract withFeaturedImageUrl(String featuredImageUrl) {
        this.featuredImageUrl = featuredImageUrl;
        return this;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public AssetContract withFeatured(boolean featured) {
        this.featured = featured;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssetContract withDescription(String description) {
        this.description = description;
        return this;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public AssetContract withExternalLink(String externalLink) {
        this.externalLink = externalLink;
        return this;
    }

    public Object getWikiLink() {
        return wikiLink;
    }

    public void setWikiLink(Object wikiLink) {
        this.wikiLink = wikiLink;
    }

    public AssetContract withWikiLink(Object wikiLink) {
        this.wikiLink = wikiLink;
        return this;
    }

    public Object getStats() {
        return stats;
    }

    public void setStats(Object stats) {
        this.stats = stats;
    }

    public AssetContract withStats(Object stats) {
        this.stats = stats;
        return this;
    }

    public Object getTraits() {
        return traits;
    }

    public void setTraits(Object traits) {
        this.traits = traits;
    }

    public AssetContract withTraits(Object traits) {
        this.traits = traits;
        return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public AssetContract withHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getNftVersion() {
        return nftVersion;
    }

    public void setNftVersion(String nftVersion) {
        this.nftVersion = nftVersion;
    }

    public AssetContract withNftVersion(String nftVersion) {
        this.nftVersion = nftVersion;
        return this;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public AssetContract withSchemaName(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    public DisplayData getDisplayData() {
        return displayData;
    }

    public void setDisplayData(DisplayData displayData) {
        this.displayData = displayData;
    }

    public AssetContract withDisplayData(DisplayData displayData) {
        this.displayData = displayData;
        return this;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public AssetContract withShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }

    public Object getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(Object totalSupply) {
        this.totalSupply = totalSupply;
    }

    public AssetContract withTotalSupply(Object totalSupply) {
        this.totalSupply = totalSupply;
        return this;
    }

    public long getBuyerFeeBasisPoints() {
        return buyerFeeBasisPoints;
    }

    public void setBuyerFeeBasisPoints(long buyerFeeBasisPoints) {
        this.buyerFeeBasisPoints = buyerFeeBasisPoints;
    }

    public AssetContract withBuyerFeeBasisPoints(long buyerFeeBasisPoints) {
        this.buyerFeeBasisPoints = buyerFeeBasisPoints;
        return this;
    }

    public long getSellerFeeBasisPoints() {
        return sellerFeeBasisPoints;
    }

    public void setSellerFeeBasisPoints(long sellerFeeBasisPoints) {
        this.sellerFeeBasisPoints = sellerFeeBasisPoints;
    }

    public AssetContract withSellerFeeBasisPoints(long sellerFeeBasisPoints) {
        this.sellerFeeBasisPoints = sellerFeeBasisPoints;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeString(imageUrl);
        dest.writeString(featuredImageUrl);
        dest.writeByte((byte) (featured ? 1 : 0));
        dest.writeString(description);
        dest.writeString(externalLink);
        dest.writeByte((byte) (hidden ? 1 : 0));
        dest.writeString(nftVersion);
        dest.writeString(schemaName);
        dest.writeString(shortDescription);
        dest.writeLong(buyerFeeBasisPoints);
        dest.writeLong(sellerFeeBasisPoints);
    }
}
