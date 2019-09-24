
package com.alphawallet.app.entity.opensea;

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

    protected Trait(Parcel in) {
        traitType = in.readString();
        value = in.readString();
    }

    public Trait(String type, String val)
    {
        traitType = type;
        value = val;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(traitType);
        dest.writeString(value);
    }
}
