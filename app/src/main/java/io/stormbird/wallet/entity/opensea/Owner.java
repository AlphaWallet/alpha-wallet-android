
package io.stormbird.wallet.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Owner implements Parcelable {

    @SerializedName("user")
    @Expose
    private Object user;
    @SerializedName("profile_img_url")
    @Expose
    private String profileImgUrl;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("config")
    @Expose
    private String config;

    protected Owner(Parcel in) {
        profileImgUrl = in.readString();
        address = in.readString();
        config = in.readString();
    }

    public static final Creator<Owner> CREATOR = new Creator<Owner>() {
        @Override
        public Owner createFromParcel(Parcel in) {
            return new Owner(in);
        }

        @Override
        public Owner[] newArray(int size) {
            return new Owner[size];
        }
    };

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }

    public Owner withUser(Object user) {
        this.user = user;
        return this;
    }

    public String getProfileImgUrl() {
        return profileImgUrl;
    }

    public void setProfileImgUrl(String profileImgUrl) {
        this.profileImgUrl = profileImgUrl;
    }

    public Owner withProfileImgUrl(String profileImgUrl) {
        this.profileImgUrl = profileImgUrl;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Owner withAddress(String address) {
        this.address = address;
        return this;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Owner withConfig(String config) {
        this.config = config;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(profileImgUrl);
        dest.writeString(address);
        dest.writeString(config);
    }
}
