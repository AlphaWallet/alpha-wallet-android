package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.stormbird.wallet.util.BalanceUtils.weiToEth;

public class Wallet implements Parcelable {
    public final String address;
    public String balance;
    public String ENSname;

	public Wallet(String address) {
		this.address = address;
		this.balance = "-";
		this.ENSname = "";
		//this.publicKey = padLeft(Numeric.cleanHexPrefix(address.toLowerCase()), 128);  //TODO: Get this from ecrecover
	}

	private Wallet(Parcel in) {
		address = in.readString();
		balance = in.readString();
		ENSname = in.readString();
		//this.publicKey = padLeft(address, 128);
	}

	public static final Creator<Wallet> CREATOR = new Creator<Wallet>() {
		@Override
		public Wallet createFromParcel(Parcel in) {
			return new Wallet(in);
		}

		@Override
		public Wallet[] newArray(int size) {
			return new Wallet[size];
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
	public void writeToParcel(Parcel parcel, int i)
	{
		parcel.writeString(address);
		parcel.writeString(balance);
		parcel.writeString(ENSname);
	}

	public void setWalletBalance(BigDecimal balanceBD)
	{
		 balance = weiToEth(balanceBD)
				.setScale(4, RoundingMode.HALF_UP)
				.stripTrailingZeros().toPlainString();
	}
}
