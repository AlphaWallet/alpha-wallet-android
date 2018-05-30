package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import static io.stormbird.wallet.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;

public class TransactionContract implements Parcelable {
    public String address;
    public String name;
    public String totalSupply;
    public int decimals;
    public String symbol;
    public boolean badTransaction;

    public TransactionContract() {
        badTransaction = false;
    }

    public int contractType()
    {
        return NORMAL_CONTRACT_TYPE;
    }

    private TransactionContract(Parcel in) {
        address = in.readString();
        name = in.readString();
        totalSupply = in.readString();
        decimals = in.readInt();
        symbol = in.readString();
        badTransaction = in.readInt() == 1;
    }

    public static final Creator<TransactionContract> CREATOR = new Creator<TransactionContract>() {
        @Override
        public TransactionContract createFromParcel(Parcel in) {
            return new TransactionContract(in);
        }

        @Override
        public TransactionContract[] newArray(int size) {
            return new TransactionContract[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(address);
        parcel.writeString(name);
        parcel.writeString(totalSupply);
        parcel.writeInt(decimals);
        parcel.writeString(symbol);
        parcel.writeInt(badTransaction ? 1 : 0);
    }
}
