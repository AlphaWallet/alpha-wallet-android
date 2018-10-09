
package io.stormbird.wallet.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Trait implements Parcelable {

    @SerializedName("trait_type")
    @Expose
    private String traitType;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("display_type")
    @Expose
    private Object displayType;
    @SerializedName("max_value")
    @Expose
    private Object maxValue;
    @SerializedName("trait_count")
    @Expose
    private long traitCount;

    protected Trait(Parcel in) {
        traitType = in.readString();
        value = in.readString();
        traitCount = in.readLong();
    }

    public static final Creator<Trait> CREATOR = new Creator<Trait>() {
        @Override
        public Trait createFromParcel(Parcel in) {
            return new Trait(in);
        }

        @Override
        public Trait[] newArray(int size) {
            return new Trait[size];
        }
    };

    public String getTraitType() {
        return traitType;
    }

    public void setTraitType(String traitType) {
        this.traitType = traitType;
    }

    public Trait withTraitType(String traitType) {
        this.traitType = traitType;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Trait withValue(String value) {
        this.value = value;
        return this;
    }

    public Object getDisplayType() {
        return displayType;
    }

    public void setDisplayType(Object displayType) {
        this.displayType = displayType;
    }

    public Trait withDisplayType(Object displayType) {
        this.displayType = displayType;
        return this;
    }

    public Object getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Object maxValue) {
        this.maxValue = maxValue;
    }

    public Trait withMaxValue(Object maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public long getTraitCount() {
        return traitCount;
    }

    public void setTraitCount(long traitCount) {
        this.traitCount = traitCount;
    }

    public Trait withTraitCount(long traitCount) {
        this.traitCount = traitCount;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(traitType);
        dest.writeString(value);
        dest.writeLong(traitCount);
    }
}
