package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import io.stormbird.wallet.service.HDKeyService;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static io.stormbird.wallet.entity.WalletType.KEYSTORE_LEGACY;
import static io.stormbird.wallet.service.HDKeyService.*;
import static io.stormbird.wallet.service.KeystoreAccountService.KEYSTORE_FOLDER;
import static io.stormbird.wallet.util.BalanceUtils.weiToEth;

public class Wallet implements Parcelable {
    public final String address;
    public String balance;
    public String ENSname;
    public String name;
    public WalletType type;
    public long lastBackupTime;
    public HDKeyService.AuthenticationLevel authLevel;

	public Wallet(String address) {
		this.address = address;
		this.balance = "-";
		this.ENSname = "";
		this.name = "";
		this.type = WalletType.NOT_DEFINED;
		this.lastBackupTime = 0;
		this.authLevel = HDKeyService.AuthenticationLevel.NOT_SET;
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
		authLevel = HDKeyService.AuthenticationLevel.values()[t];
	}

	public void setWalletType(WalletType wType)
	{
		type = wType;
	}

	public void checkWalletType(Context ctx)
	{
		if (new File(ctx.getFilesDir(), address+HDKEY_LABEL).exists() ||
				new File(ctx.getFilesDir(), address+NO_AUTH_LABEL+HDKEY_LABEL).exists())
		{
			type = WalletType.HDKEY;
		}
		else if (new File(ctx.getFilesDir(), address + KEYSTORE_LABEL).exists()
				|| new File(ctx.getFilesDir(), address + NO_AUTH_LABEL + KEYSTORE_LABEL).exists())
		{
			type = WalletType.KEYSTORE;
		}
		else if (new File(ctx.getFilesDir(), address).exists())
		{
			type = WalletType.KEYSTORE_LEGACY;
		}
		else
		{
			//assume watch wallet
			type = WalletType.WATCH;
		}
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
