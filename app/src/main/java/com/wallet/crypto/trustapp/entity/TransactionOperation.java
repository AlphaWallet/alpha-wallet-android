package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class TransactionOperation implements Parcelable {
    public String transactionId;
    public String viewType;
    public String from;
    public String to;
    public String value;
    public TransactionContract contract;

    public TransactionOperation() {

    }

    private TransactionOperation(Parcel in) {
        transactionId = in.readString();
        viewType = in.readString();
        from = in.readString();
        to = in.readString();
        value = in.readString();
        contract = in.readParcelable(TransactionContract.class.getClassLoader());
    }

    public static final Creator<TransactionOperation> CREATOR = new Creator<TransactionOperation>() {
        @Override
        public TransactionOperation createFromParcel(Parcel in) {
            return new TransactionOperation(in);
        }

        @Override
        public TransactionOperation[] newArray(int size) {
            return new TransactionOperation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(transactionId);
        parcel.writeString(viewType);
        parcel.writeString(from);
        parcel.writeString(to);
        parcel.writeString(value);
        parcel.writeParcelable(contract, flags);
    }
}
