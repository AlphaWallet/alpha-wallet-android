package com.wallet.crypto.trustapp.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class Account implements Parcelable {
    public final String address;

	public Account(String address) {
		this.address = address;
	}

	private Account(Parcel in) {
		address = in.readString();
	}

	public static final Creator<Account> CREATOR = new Creator<Account>() {
		@Override
		public Account createFromParcel(Parcel in) {
			return new Account(in);
		}

		@Override
		public Account[] newArray(int size) {
			return new Account[size];
		}
	};

	public boolean sameAddress(String address) {
		return this.address.equals(address);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(address);
	}
}
