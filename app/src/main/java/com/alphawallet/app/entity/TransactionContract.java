package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;
import java.util.List;

import static com.alphawallet.app.entity.TransactionOperation.NORMAL_CONTRACT_TYPE;

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

    public TransactionType getOperationType()
    {
        return TransactionType.UNKNOWN;
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

    public boolean checkAddress(String address)
    {
        return false; //this style of contract doesn't involve indirect addresses (only ERC875 'trade' and 'passTo' functions).
    }

    public void interpretTradeData(String walletAddr, Transaction thisTrans)
    {
        //do nothing for ERC20
    }

    public void interpretTransferFrom(String walletAddr, Transaction trans)
    {

    }

    public void interpretTransfer(String walletAddr)
    {

    }

    public void setOtherParty(String otherParty)
    {

    }

    public void setOperation(TransactionType operation)
    {
        if (operation == TransactionType.INVALID_OPERATION)
        {
            badTransaction = true;
        }
    }

    public void completeSetup(String currentAddress, Transaction trans)
    {

    }

    public void interpretPassTo(String walletAddr, Transaction transaction)
    {

    }

    public void setType(int type)
    {

    }

    public void setIndicies(List<BigInteger> indicies)
    {

    }

    public String getIndicesString()
    {
        return "";
    }

    public String getOperationName(Context ctx)
    {
        String result = null;
        if (name != null && name.length() > 0 && name.charAt(0) == '*')
        {
            int operationType = Integer.parseInt(name.substring(1));
            result = ctx.getString(TransactionLookup.typeToName(TransactionType.values()[operationType]));
        }
        else if (name != null && name.length() > 0)
        {
            result = name;
        }
        return result;
    }

    public String getIndicesSize()
    {
        return "0";
    }
}
