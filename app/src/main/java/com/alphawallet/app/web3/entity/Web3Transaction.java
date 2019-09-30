package com.alphawallet.app.web3.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;

public class Web3Transaction implements Parcelable {
    public final Address recipient;
    public final Address contract;
    public final BigInteger value;
    public final BigInteger gasPrice;
    public final BigInteger gasLimit;
    public final long nonce;
    public final String payload;
    public final long leafPosition;

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload) {
        this(recipient, contract, value, gasPrice, gasLimit, nonce, payload, 0);
    }

    public Web3Transaction(
            Address recipient,
            Address contract,
            BigInteger value,
            BigInteger gasPrice,
            BigInteger gasLimit,
            long nonce,
            String payload,
            long leafPosition) {
        this.recipient = recipient;
        this.contract = contract;
        this.value = value;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.nonce = nonce;
        this.payload = payload;
        this.leafPosition = leafPosition;
    }

    Web3Transaction(Parcel in) {
        recipient = in.readParcelable(Address.class.getClassLoader());
        contract = in.readParcelable(Address.class.getClassLoader());
        value = new BigInteger(in.readString());
        gasPrice = new BigInteger(in.readString());
        gasLimit = new BigInteger(in.readString());
        nonce = in.readLong();
        payload = in.readString();
        leafPosition = in.readLong();
    }

    public static final Creator<Web3Transaction> CREATOR = new Creator<Web3Transaction>() {
        @Override
        public Web3Transaction createFromParcel(Parcel in) {
            return new Web3Transaction(in);
        }

        @Override
        public Web3Transaction[] newArray(int size) {
            return new Web3Transaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(recipient, flags);
        dest.writeParcelable(contract, flags);
        dest.writeString((value == null ? BigInteger.ZERO : value).toString());
        dest.writeString((gasPrice == null ? BigInteger.ZERO : gasPrice).toString());
        dest.writeString((gasLimit == null ? BigInteger.ZERO : gasLimit).toString());
        //dest.writeLong(gasLimit);
        dest.writeLong(nonce);
        dest.writeString(payload);
        dest.writeLong(leafPosition);
    }
}
