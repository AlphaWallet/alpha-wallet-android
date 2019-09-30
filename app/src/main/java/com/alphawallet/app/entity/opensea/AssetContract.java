
package com.alphawallet.app.entity.opensea;

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

    @SerializedName("schema_name")
    @Expose
    private String schemaName;

    protected AssetContract(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        schemaName = in.readString();
    }

    public AssetContract(String address, String name, String symbol, String schemaName)
    {
        this.address = address;
        this.name = name;
        this.symbol = symbol;
        this.schemaName = schemaName;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeString(schemaName);
    }
}
