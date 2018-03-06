package com.wallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 4/03/2018.
 */

//TODO: there should be an abstract base class, all contracts inherit from it, only contains address, name and symbol
public class ERC875ContractTransaction extends TransactionContract implements Parcelable {
    public String address;
    public String name;
    public String balance;
    public String symbol;
    public String operation;
    public String otherParty;
    public List<Integer> indicies;
    public int type;

    public ERC875ContractTransaction() {

    }

    public void addIndicies(List<BigInteger> indicies)
    {
        this.indicies = new ArrayList<>();
        for (BigInteger index : indicies)
        {
            this.indicies.add(index.intValue());
        }
    }

    private ERC875ContractTransaction(Parcel in) {
        address = in.readString();
        name = in.readString();
        balance = in.readString();
        symbol = in.readString();
        operation = in.readString();
        type = in.readInt();
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        indicies = new ArrayList<Integer>();
        for (Object o : readObjArray)
        {
            Integer val = (Integer)o;
            indicies.add(val);
        }
    }

    public static final Creator<ERC875ContractTransaction> CREATOR = new Creator<ERC875ContractTransaction>() {
        @Override
        public ERC875ContractTransaction createFromParcel(Parcel in) {
            return new ERC875ContractTransaction(in);
        }

        @Override
        public ERC875ContractTransaction[] newArray(int size) {
            return new ERC875ContractTransaction[size];
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
        parcel.writeString(balance);
        parcel.writeString(symbol);
        parcel.writeString(operation);
        parcel.writeInt(type);
        parcel.writeArray(indicies.toArray());
    }
}
