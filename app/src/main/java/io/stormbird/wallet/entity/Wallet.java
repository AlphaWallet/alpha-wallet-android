package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import io.stormbird.wallet.service.KeyService;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.stormbird.wallet.service.KeyService.*;
import static io.stormbird.wallet.util.BalanceUtils.weiToEth;

public class Wallet implements Parcelable {
    public final String address;
    public String balance;
    public String ENSname;
    public String name;
    public WalletType type;
    public long lastBackupTime;
    public KeyService.AuthenticationLevel authLevel;

	public Wallet(String address) {
		this.address = address;
		this.balance = "-";
		this.ENSname = "";
		this.name = "";
		this.type = WalletType.NOT_DEFINED;
		this.lastBackupTime = 0;
		this.authLevel = KeyService.AuthenticationLevel.NOT_SET;
	}

	private Wallet(Parcel in)
	{
		address = in.readString();
		balance = in.readString();
		ENSname = in.readString();
		name = in.readString();
		int t = in.readInt();
		type = WalletType.values()[t];
		lastBackupTime = in.readLong();
		t = in.readInt();
		authLevel = KeyService.AuthenticationLevel.values()[t];
	}

	public void setWalletType(WalletType wType)
	{
		type = wType;
	}

	public void checkWalletType(Context ctx)
	{
		type = getKeystoreType(ctx, address);
		authLevel = getAuthLevel(ctx, address);
	}

	public static WalletType getKeystoreType(Context context, String addr)
	{
		if (addr == null) return WalletType.NOT_DEFINED;
		else if (new File(context.getFilesDir(), addr+HDKEY_LABEL).exists() ||
				new File(context.getFilesDir(), addr+NO_AUTH_LABEL+HDKEY_LABEL).exists()) return  WalletType.HDKEY;
		else if (new File(context.getFilesDir(), addr + KEYSTORE_LABEL).exists()
				|| new File(context.getFilesDir(), addr + NO_AUTH_LABEL + KEYSTORE_LABEL).exists()) return WalletType.KEYSTORE;
		else if (new File(context.getFilesDir(), addr).exists()) return WalletType.KEYSTORE_LEGACY;
		else return WalletType.WATCH;
	}
	private static AuthenticationLevel getAuthLevel(Context context, String addr)
	{
		if (new File(context.getFilesDir(), addr+HDKEY_LABEL).exists() ||
				new File(context.getFilesDir(), addr+KEYSTORE_LABEL).exists())
			return KeyService.hasStrongbox() ? AuthenticationLevel.STRONGBOX_AUTHENTICATION : AuthenticationLevel.TEE_AUTHENTICATION;
		else if (new File(context.getFilesDir(), addr + NO_AUTH_LABEL + HDKEY_LABEL).exists()
				|| new File(context.getFilesDir(), addr + NO_AUTH_LABEL + KEYSTORE_LABEL).exists()
				|| new File(context.getFilesDir(), addr).exists())
			return KeyService.hasStrongbox() ? AuthenticationLevel.STRONGBOX_NO_AUTHENTICATION : AuthenticationLevel.TEE_NO_AUTHENTICATION;
		else return AuthenticationLevel.NOT_SET;
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
		parcel.writeString(name);
		parcel.writeInt(type.ordinal());
		parcel.writeLong(lastBackupTime);
		parcel.writeInt(authLevel.ordinal());
	}

	public void setWalletBalance(BigDecimal balanceBD)
	{
		 balance = weiToEth(balanceBD)
				.setScale(4, RoundingMode.HALF_DOWN)
				.stripTrailingZeros().toPlainString();
	}
}
