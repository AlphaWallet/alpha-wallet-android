package io.awallet.crypto.alphawallet.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.awallet.crypto.alphawallet.entity.TransactionOperation.ERC875_CONTRACT_TYPE;

/**
 * Created by James on 4/03/2018.
 */

//TODO: there should be an abstract base class, all contracts inherit from it, only contains address, name and symbol
public class ERC875ContractTransaction extends TransactionContract implements Parcelable {
    public String balance;
    public String symbol;
    public String operation;
    public String otherParty;
    public List<Integer> indices;
    public int type;

    @Override
    public int contractType()
    {
        return ERC875_CONTRACT_TYPE;
    }

    public ERC875ContractTransaction() {
        address = "";
        name = "";
        balance = "";
        symbol = "";
        operation = "";
        otherParty = "";
        indices = null;
    }

    public void setIndicies(List<BigInteger> indices)
    {
        this.indices = new ArrayList<>();
        for (BigInteger index : indices)
        {
            this.indices.add(index.intValue());
        }
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
        operation = in.readString();
        type = in.readInt();
        Object[] readObjArray = in.readArray(Object.class.getClassLoader());
        indices = new ArrayList<>();
        for (Object o : readObjArray)
        {
            Integer val = (Integer)o;
            indices.add(val);
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
        if (indices != null) {
            parcel.writeArray(indices.toArray());
        }
    }

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
}
