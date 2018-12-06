package io.stormbird.wallet.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;

/* weiwu: I think this is what intended:
   a transaction is a single unit of logic or work, sometimes made up of multiple operations.
 */
public class TransactionOperation implements Parcelable {
    public String transactionId;
    public String viewType;
    public String from;
    public String to;
    public String value;
    public TransactionContract contract;

    public static final int NORMAL_CONTRACT_TYPE = 20;
    public static final int ERC875_CONTRACT_TYPE = 875;

    public TransactionOperation() {

    }

    private TransactionOperation(Parcel in) {
        transactionId = in.readString();
        viewType = in.readString();
        from = in.readString();
        to = in.readString();
        value = in.readString();
        int type = in.readInt();
        switch (type)
        {
            case NORMAL_CONTRACT_TYPE:
                contract = in.readParcelable(TransactionContract.class.getClassLoader());
                break;
            case ERC875_CONTRACT_TYPE:
                contract = in.readParcelable(ERC875ContractTransaction.class.getClassLoader());
                break;
        }
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
        parcel.writeInt(contract.contractType());
        parcel.writeParcelable(contract, flags);
    }

    public boolean walletInvolvedWithTransaction(String address)
    {
        if (contract != null)
        {
            return contract.checkAddress(address);
        }
        else
        {
            return false;
        }
    }

    public String getOperationName(Context ctx)
    {
        if (contract != null)
        {
            return contract.getOperationName(ctx);
        }
        else
        {
            return null;
        }
    }

    public String getValue(int decimals)
    {
        if (decimals > 1 && value != null && value.length() > 0 && Character.isDigit(value.charAt(0)))
        {
            return Token.getScaledValue(value, decimals);
        }
        else if (contract instanceof ERC875ContractTransaction)
        {
            if (value != null && value.length() > 0 && Character.isDigit(value.charAt(0)))
            {
                BigInteger val = new BigInteger(value);
                if (val.compareTo(BigInteger.valueOf(65535L)) > 0) //This can be removed after the next build
                {
                    //index out of range
                    return contract.getIndicesSize();
                }
            }
            return contract.getIndicesSize();
        }
        else if (value != null)
        {
            return value;
        }
        else
        {
            return "0";
        }
    }
}
