package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

public class Transaction implements Parcelable {
    @SerializedName("id")
    public final String hash;
    public final String blockNumber;
    public final long timeStamp;
    public final int nonce;
    public final String from;
    public final String to;
    public final String value;
    public final String gas;
    public final String gasPrice;
    public final String gasUsed;
    public final String input;
    public final TransactionOperation[] operations;
    public final String error;

    public Transaction(
            String hash,
            String error,
            String blockNumber,
            long timeStamp,
			int nonce,
			String from,
			String to,
			String value,
			String gas,
			String gasPrice,
			String input,
			String gasUsed,
            TransactionOperation[] operations) {
        this.hash = hash;
        this.error = error;
        this.blockNumber = blockNumber;
        this.timeStamp = timeStamp;
		this.nonce = nonce;
		this.from = from;
		this.to = to;
		this.value = value;
		this.gas = gas;
		this.gasPrice = gasPrice;
		this.input = input;
		this.gasUsed = gasUsed;
		this.operations = operations;
	}

	protected Transaction(Parcel in) {
        hash = in.readString();
        error = in.readString();
        blockNumber = in.readString();
        timeStamp = in.readLong();
		nonce = in.readInt();
		from = in.readString();
		to = in.readString();
		value = in.readString();
		gas = in.readString();
		gasPrice = in.readString();
		input = in.readString();
		gasUsed = in.readString();
        Parcelable[] parcelableArray = in.readParcelableArray(TransactionOperation.class.getClassLoader());
        TransactionOperation[] operations = null;
        if (parcelableArray != null) {
            operations = Arrays.copyOf(parcelableArray, parcelableArray.length, TransactionOperation[].class);
        }
		this.operations = operations;
	}

	public static final Creator<Transaction> CREATOR = new Creator<Transaction>() {
		@Override
		public Transaction createFromParcel(Parcel in) {
			return new Transaction(in);
		}

		@Override
		public Transaction[] newArray(int size) {
			return new Transaction[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hash);
        dest.writeString(error);
        dest.writeString(blockNumber);
        dest.writeLong(timeStamp);
		dest.writeInt(nonce);
		dest.writeString(from);
		dest.writeString(to);
		dest.writeString(value);
		dest.writeString(gas);
		dest.writeString(gasPrice);
		dest.writeString(input);
		dest.writeString(gasUsed);
		dest.writeParcelableArray(operations, flags);
	}
}
