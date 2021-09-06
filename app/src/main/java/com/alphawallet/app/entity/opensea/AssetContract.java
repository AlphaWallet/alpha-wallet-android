
package com.alphawallet.app.entity.opensea;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.util.DateTimeFactory;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import static io.realm.Realm.getApplicationContext;

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

    @SerializedName("image_url")
    @Expose
    private String imageUrl;

    @SerializedName("created_date")
    @Expose
    private String creationDate;

    @SerializedName("description")
    @Expose
    private String description;

    protected AssetContract(Parcel in) {
        address = in.readString();
        name = in.readString();
        symbol = in.readString();
        schemaName = in.readString();
        creationDate = in.readString();
        description = in.readString();
    }

    public AssetContract(Token token)
    {
        this.address = token.tokenInfo.address;
        this.name = token.tokenInfo.name;
        this.symbol = token.tokenInfo.symbol;
        this.schemaName = token.getInterfaceSpec().toString();
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

    public String getImageUrl() { return imageUrl; }

    public String getCreationDate()
    {
        try
        {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.ROOT); //new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.mmmmmm", Locale.ROOT);
            Date dd = formatter.parse(creationDate);
            DateFormat timeFormat = java.text.DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtils.getDeviceLocale(getApplicationContext()));
            DateFormat dateFormat = java.text.DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtils.getDeviceLocale(getApplicationContext()));
            return dateFormat.format(dd) + " " + timeFormat.format(dd);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public String getDescription()
    {
        return description;
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
        dest.writeString(creationDate);
        dest.writeString(description);
    }

    public String getJSON()
    {
        JSONObject jsonData = new JSONObject();

        try
        {
            jsonData.put("address", address);
            jsonData.put("name", name);
            jsonData.put("symbol", symbol);
            jsonData.put("schema_name", schemaName);
            jsonData.put("created_date", creationDate);
            jsonData.put("description", description);
        }
        catch (JSONException e)
        {
            //
        }

        return jsonData.toString();
    }
}
