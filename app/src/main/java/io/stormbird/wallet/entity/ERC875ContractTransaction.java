package io.stormbird.wallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.R;

import static io.stormbird.wallet.entity.TransactionOperation.ERC875_CONTRACT_TYPE;

/**
 * Created by James on 4/03/2018.
 */

//TODO: there should be an abstract base class, all contracts inherit from it, only contains address, name and symbol
public class ERC875ContractTransaction extends TransactionContract implements Parcelable {
    public String balance;
    public String symbol;
    public TransactionType operation;
    public String otherParty;
    public List<Integer> indices;
    public int type;

    @Override
    public int contractType()
    {
        return ERC875_CONTRACT_TYPE;
    }

    public ERC875ContractTransaction()
    {
        address = "";
        name = "";
        balance = "";
        symbol = "";
        operation = TransactionType.UNKNOWN;
        otherParty = "";
        //operationDisplayName = "";
        indices = null;
    }

    public void setIndicesFromString(String indicesStr)
    {
        this.indices = new ArrayList<>();
        try
        {
            String[] indicesArray = indicesStr.split(",");

            for (String index : indicesArray)
            {
                //remove whitespace
                String trim = index.trim();
                indices.add(Integer.parseInt(trim));
            }
        }
        catch (Exception e)
        {

        }
    }

    private ERC875ContractTransaction(Parcel in) {
        address = in.readString();
        name = in.readString();
        balance = in.readString();
        symbol = in.readString();
        int typeCode = in.readInt();
        if (typeCode >= TransactionType.ILLEGAL_VALUE.ordinal()) typeCode = TransactionType.ILLEGAL_VALUE.ordinal();
        operation = TransactionType.values()[typeCode];
        type = in.readInt();
        otherParty = in.readString();
        //operationDisplayName = in.readString();
        int arrayCount = in.readInt();
        indices = new ArrayList<>();
        if (arrayCount > 0)
        {
            Object[] readObjArray = in.readArray(Object.class.getClassLoader());

            for (Object o : readObjArray)
            {
                Integer val = (Integer) o;
                indices.add(val);
            }
        }
    }

    public static final Creator<ERC875ContractTransaction> CREATOR = new Creator<ERC875ContractTransaction>()
    {
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
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(address);
        parcel.writeString(name);
        parcel.writeString(balance);
        parcel.writeString(symbol);
        int opWrite = operation.ordinal();
        parcel.writeInt(opWrite);
        parcel.writeInt(type);
        parcel.writeString(otherParty);

        if (indices != null)
        {
            parcel.writeInt(indices.size());
            parcel.writeArray(indices.toArray());
        }
        else
        {
            parcel.writeInt(0);
        }
    }

    @Override
    public String getIndicesString()
    {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        if (indices != null)
        {
            for (Integer id : indices)
            {
                if (!first)
                {
                    sb.append(", ");
                }
                first = false;

                sb.append(id.toString());
            }
        }

        return sb.toString();
    }

    @Override
    public boolean checkAddress(String address)
    {
        return otherParty != null && otherParty.equals(address);
    }

    @Override
    public void interpretTradeData(String walletAddr, Transaction thisTrans)
    {
        BigInteger priceWei = new BigInteger(thisTrans.value);
        if (priceWei.equals(BigInteger.ZERO))
        {
            if (otherParty.equals(walletAddr))
            {
                //transfered out of our wallet via magic link
                operation = TransactionType.MAGICLINK_TRANSFER;// R.string.ticket_magiclink_transfer;
                type = -1;
            }
            else
            {
                //received ticket from a magic link
                operation = TransactionType.MAGICLINK_PICKUP;// R.string.ticket_magiclink_pickup;
                type = 1;
            }
        }
        else
        {
            if (otherParty.equals(walletAddr))
            {
                //we received ether from magiclink sale
                operation = TransactionType.MAGICLINK_SALE;// R.string.ticket_magiclink_sale;
                type = -1;
            }
            else
            {
                //we purchased a ticket from a magiclink
                operation = TransactionType.MAGICLINK_PURCHASE;// R.string.ticket_magiclink_purchase;
                type = 1;
            }
        }
    }

    @Override
    public void interpretTransferFrom(String walletAddr, TransactionInput data)
    {
        operation = TransactionType.REDEEM;//R.string.ticket_redeem;
        if (!data.containsAddress(walletAddr))
        {
            //this must be an admin redeem
            operation = TransactionType.ADMIN_REDEEM;// R.string.ticket_admin_redeem;
        }
        //one of our tickets was burned
        type = -1; //redeem
    }

    @Override
    public void interpretTransfer(String walletAddr, TransactionInput data)
    {
        //this could be transfer to or from
        //if addresses contains our address then it must be a recieve
        if (data.containsAddress(walletAddr))
        {
            operation = TransactionType.RECEIVE_FROM;// R.string.ticket_receive_from;
            type = 1; //buy/receive
        }
        else
        {
            operation = TransactionType.TRANSFER_TO;// R.string.ticket_transfer_to;
            type = -1; //sell
            otherParty = data.getFirstAddress();
        }
    }

    @Override
    public void setOtherParty(String otherParty)
    {
        this.otherParty = otherParty;
    }

    @Override
    public void setOperation(TransactionType operation)
    {
        super.setOperation(operation);
        this.operation = operation;
    }

    @Override
    public void setType(int type)
    {
        this.type = type;
    }

    @Override
    public void interpretPassTo(String walletAddr, TransactionInput data)
    {
        if (data.containsAddress(walletAddr))
        {
            //we received a ticket from magiclink with transfer paid by server
            operation = TransactionType.PASS_FROM;// R.string.ticket_pass_from;
            type = 1;
        }
        else
        {
            //we had a ticket transferred out of our wallet paid for by server.
            //This will not show up unless we ecrecover the address.
            operation = TransactionType.PASS_TO;//R.string.ticket_pass_to;
            type = -1;
        }
    }

    @Override
    public void setIndicies(List<BigInteger> indices)
    {
        this.indices = new ArrayList<>();
        for (BigInteger index : indices)
        {
            this.indices.add(index.intValue());
        }
    }
}
