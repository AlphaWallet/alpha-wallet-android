package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class Message<V> implements Parcelable {

    public final V value;
    public final String url;
    public final long leafPosition;

    public Message(V value, String url, long leafPosition) {
        this.value = value;
        this.url = url;
        this.leafPosition = leafPosition;
    }

    protected Message(Parcel in) {
        Class<?> type = (Class<?>) in.readSerializable();
        value = (V) in.readValue(type.getClassLoader());
        url = in.readString();
        leafPosition = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(value.getClass());
        dest.writeValue(value);
        dest.writeString(url);
        dest.writeLong(leafPosition);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
