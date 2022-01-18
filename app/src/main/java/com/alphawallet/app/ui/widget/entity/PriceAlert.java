package com.alphawallet.app.ui.widget.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class PriceAlert implements Parcelable {
    private String value;
    private String currency;
    private String token;
    private String address;
    private long chainId;
    private boolean isAbove;
    private boolean enabled;

    public PriceAlert(String currency, String token, String address, long chainId)
    {
        this.currency = currency;
        this.token = token;
        this.isAbove = true;
        this.enabled = true;
        this.address = address;
        this.chainId = chainId;
    }

    protected PriceAlert(Parcel in)
    {
        value = in.readString();
        currency = in.readString();
        token = in.readString();
        isAbove = in.readByte() != 0;
        enabled = in.readByte() != 0;
        address = in.readString();
        chainId = in.readLong();
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean getAbove()
    {
        return isAbove;
    }

    public void setAbove(boolean above)
    {
        this.isAbove = above;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(value);
        dest.writeString(currency);
        dest.writeString(token);
        dest.writeByte((byte) (isAbove ? 1 : 0));
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeString(address);
        dest.writeLong(chainId);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<PriceAlert> CREATOR = new Creator<PriceAlert>() {
        @Override
        public PriceAlert createFromParcel(Parcel in)
        {
            return new PriceAlert(in);
        }

        @Override
        public PriceAlert[] newArray(int size)
        {
            return new PriceAlert[size];
        }
    };

    public boolean match(Double rate, double currentTokenPrice) {
        return (getAbove() && currentTokenPrice * rate > Double.parseDouble(getValue())) ||
                (!getAbove() && currentTokenPrice * rate < Double.parseDouble(getValue()));
    }
}
