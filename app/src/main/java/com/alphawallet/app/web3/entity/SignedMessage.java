package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class SignedMessage<V> extends Message<V> implements Parcelable {

    public final String signHex;

    public SignedMessage(Message<V> unsignedMessage, String signHex) {
        super(unsignedMessage.value, unsignedMessage.url, unsignedMessage.leafPosition);
        this.signHex = signHex;
    }

    public SignedMessage(Parcel in) {
        super(in);
        signHex = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(signHex);
    }

    public static final Creator<SignedMessage> CREATOR = new Creator<SignedMessage>() {
        @Override
        public SignedMessage createFromParcel(Parcel in) {
            return new SignedMessage(in);
        }

        @Override
        public SignedMessage[] newArray(int size) {
            return new SignedMessage[size];
        }
    };
}
