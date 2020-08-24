package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class LocaleItem implements Parcelable
{
    public String name;
    public String code;
    public boolean isSelected;

    public LocaleItem(String name, boolean isSelected) {
        this.name = name;
        this.isSelected = isSelected;
    }

    public LocaleItem(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public LocaleItem(String name, String code, boolean isSelected) {
        this.name = name;
        this.code = code;
        this.isSelected = isSelected;
    }

    public LocaleItem(Parcel in)
    {
        name = in.readString();
        code = in.readString();
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
        dest.writeInt(isSelected?1:0);
    }

    public static final Parcelable.Creator<LocaleItem> CREATOR = new Parcelable.Creator<LocaleItem>() {
        @Override
        public LocaleItem createFromParcel(Parcel in) {
            return new LocaleItem(in);
        }

        @Override
        public LocaleItem[] newArray(int size) {
            return new LocaleItem[size];
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
}
