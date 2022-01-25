package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.service.TickerService;

public class CurrencyItem implements Parcelable
{
    private String code;
    private String name;
    private final String symbol;
    private int flag = -1;
    public boolean isSelected;

    public CurrencyItem(String code, String name, String symbol, int flag) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.flag = flag;
    }

    public CurrencyItem(Parcel in)
    {
        name = in.readString();
        code = in.readString();
        symbol = in.readString();
        flag = in.readInt();
        isSelected = in.readInt() == 1;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name);
        dest.writeString(code);
        dest.writeString(symbol);
        dest.writeInt(flag);
        dest.writeInt(isSelected?1:0);
    }

    public static final Creator<CurrencyItem> CREATOR = new Creator<CurrencyItem>() {
        @Override
        public CurrencyItem createFromParcel(Parcel in) {
            return new CurrencyItem(in);
        }

        @Override
        public CurrencyItem[] newArray(int size) {
            return new CurrencyItem[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getFlag() {
        return flag;
    }

    public String getCurrencyText(double value) {
        return TickerService.getCurrencyWithoutSymbol(value) + " " + getCode();
    }
}
