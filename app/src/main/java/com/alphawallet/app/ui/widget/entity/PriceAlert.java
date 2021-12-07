package com.alphawallet.app.ui.widget.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class PriceAlert implements Parcelable {
    private String value;
    private String currency;
    private String token;
    // true - means, rises above / false - means, drops to
    private boolean indicator;
    private boolean enabled;

    public PriceAlert(String currency, String token)
    {
        this.currency = currency;
        this.token = token;
        this.indicator = true;
        this.enabled = true;
    }

    public PriceAlert(String value, String currency, String token)
    {
        this.value = value;
        this.currency = currency;
        this.token = token;
        this.indicator = true;
        this.enabled = true;
    }

    protected PriceAlert(Parcel in)
    {
        value = in.readString();
        currency = in.readString();
        token = in.readString();
        indicator = in.readByte() != 0;
        enabled = in.readByte() != 0;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean getIndicator()
    {
        return indicator;
    }

    public void setIndicator(boolean indicator)
    {
        this.indicator = indicator;
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

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(value);
        dest.writeString(currency);
        dest.writeString(token);
        dest.writeByte((byte) (indicator ? 1 : 0));
        dest.writeByte((byte) (enabled ? 1 : 0));
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
}
