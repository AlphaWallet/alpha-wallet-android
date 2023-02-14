package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.alphawallet.app.util.Hex;


public class Address implements Parcelable {

    public static final Address EMPTY = new Address("0000000000000000000000000000000000000000");

    private final String value;

    public Address(@NonNull String value) {
        value = value.toLowerCase();
        if (Hex.containsHexPrefix(value)) {
            value = Hex.cleanHexPrefix(value);
        }
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Address can't null.");
        }
        this.value = value;
    }

    protected Address(Parcel in) {
        value = in.readString();
    }

    @Override
    public String toString() {
        return "0x" + value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Address && value.equalsIgnoreCase(((Address) other).value);

    }

    public static final Creator<Address> CREATOR = new Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel in) {
            return new Address(in);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(value);
    }
}
