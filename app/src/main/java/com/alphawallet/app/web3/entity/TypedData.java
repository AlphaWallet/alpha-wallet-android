package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class TypedData implements Parcelable
{
    public final String name;
    public final String type;
    public final Object data;

    public TypedData(String name, String type, Object data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    protected TypedData(Parcel in) {
        name = in.readString();
        type = in.readString();
        Class<?> type = (Class<?>) in.readSerializable();
        data = in.readValue(type.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(type);
        dest.writeSerializable(data.getClass());
        dest.writeValue(data);
    }

    public static final Creator<TypedData> CREATOR = new Creator<TypedData>() {
        @Override
        public TypedData createFromParcel(Parcel in) {
            return new TypedData(in);
        }

        @Override
        public TypedData[] newArray(int size) {
            return new TypedData[size];
        }
    };
}
