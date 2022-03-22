package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.annotations.PrimaryKey;

public class AddressBookContact implements Parcelable
{
    /**
     * This is contact wallet address
     */
    @PrimaryKey
    private String walletAddress;

    /**
     * This is contact name
     */
    private String name;

    /**
     * This is associated ETH name of #walletAddress
     */
    private String ethName;

    public String getWalletAddress() {
        return walletAddress;
    }

    public AddressBookContact(String walletAddress, String name, String ethName)
    {
        this.walletAddress = walletAddress;
        this.name = name;
        this.ethName = ethName;
    }

    private AddressBookContact(Parcel in) {
        walletAddress = in.readString();
        name = in.readString();
        ethName = in.readString();
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEthName() {
        return ethName;
    }

    public void setEthName(String ethName) {
        this.ethName = ethName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(walletAddress);
        dest.writeString(name);
        dest.writeString(ethName);
    }

    public static final Creator<AddressBookContact> CREATOR = new Creator<AddressBookContact>() {
        @Override
        public AddressBookContact createFromParcel(Parcel source) {
            return new AddressBookContact(source);
        }

        @Override
        public AddressBookContact[] newArray(int size) {
            return new AddressBookContact[size];
        }
    };

    @Override
    public String toString() {
        return "UserInfo{" +
                "walletAddress='" + walletAddress + '\'' +
                ", name='" + name + '\'' +
                ", ethName='" + ethName + '\'' +
                '}';
    }
}
