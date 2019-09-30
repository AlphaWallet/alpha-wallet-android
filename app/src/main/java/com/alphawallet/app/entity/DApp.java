package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class DApp implements Parcelable {
    String name;
    String url;
    String category;
    String description;
    boolean added;

    public DApp(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    protected DApp(Parcel in) {
        name = in.readString();
        url = in.readString();
        category = in.readString();
        description = in.readString();
        added = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(url);
        dest.writeString(category);
        dest.writeString(description);
        dest.writeByte((byte) (added ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DApp> CREATOR = new Creator<DApp>() {
        @Override
        public DApp createFromParcel(Parcel in) {
            return new DApp(in);
        }

        @Override
        public DApp[] newArray(int size) {
            return new DApp[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DApp)) return false;
        DApp dApp = (DApp) o;
        if (url == null || name == null || dApp.name == null || dApp.url == null) return false;
        return name.equals(dApp.name) && url.equals( dApp.url);
    }

    @Override
    public int hashCode() {
        Object[] a = { name, url };
        int result = 1;
        for (Object element : a)
            result = 31 * result + (element == null ? 0 : element.hashCode());
        return result;
    }
}
