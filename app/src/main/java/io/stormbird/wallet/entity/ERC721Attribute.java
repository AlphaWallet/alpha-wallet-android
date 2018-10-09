package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class ERC721Attribute implements Parcelable {
    private String traitType;
    private String value;
    private String displayType;
    private String maxValue;
    private long traitCount;

    public String getTraitType() {
        return traitType;
    }

    public void setTraitType(String traitType) {
        this.traitType = traitType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDisplayType() {
        return displayType;
    }

    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(String maxValue) {
        this.maxValue = maxValue;
    }

    public long getTraitCount() {
        return traitCount;
    }

    public void setTraitCount(long traitCount) {
        this.traitCount = traitCount;
    }

    public ERC721Attribute(String traitType, String value) {
        this.traitType = traitType;
        this.value = value;
    }

    protected ERC721Attribute(Parcel in) {
        traitType = in.readString();
        value = in.readString();
        displayType = in.readString();
        maxValue = in.readString();
        traitCount = in.readLong();
    }

    public static final Creator<ERC721Attribute> CREATOR = new Creator<ERC721Attribute>() {
        @Override
        public ERC721Attribute createFromParcel(Parcel in) {
            return new ERC721Attribute(in);
        }

        @Override
        public ERC721Attribute[] newArray(int size) {
            return new ERC721Attribute[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(traitType);
        dest.writeString(value);
        dest.writeString(displayType);
        dest.writeString(maxValue);
        dest.writeLong(traitCount);
    }
}
