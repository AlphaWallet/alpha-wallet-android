package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;

/**
 * Created by JB on 20/01/2022.
 */
public class EIP1559FeeOracleResult implements Parcelable
{
    public final BigInteger maxFeePerGas;
    public final BigInteger maxPriorityFeePerGas;
    public final String name;

    public EIP1559FeeOracleResult(BigInteger maxFee, BigInteger maxPriority)
    {
        maxFeePerGas = maxFee;
        maxPriorityFeePerGas = maxPriority;
        name = null;
    }

    public EIP1559FeeOracleResult(EIP1559FeeOracleResult r, String n)
    {
        maxFeePerGas = r.maxFeePerGas;
        maxPriorityFeePerGas = r.maxPriorityFeePerGas;
        name = n;
    }

    protected EIP1559FeeOracleResult(Parcel in)
    {
        maxFeePerGas = new BigInteger(in.readString(), 16);
        maxPriorityFeePerGas = new BigInteger(in.readString(), 16);
        name = in.readString();
    }

    public static final Creator<EIP1559FeeOracleResult> CREATOR = new Creator<EIP1559FeeOracleResult>() {
        @Override
        public EIP1559FeeOracleResult createFromParcel(Parcel in) {
            return new EIP1559FeeOracleResult(in);
        }

        @Override
        public EIP1559FeeOracleResult[] newArray(int size) {
            return new EIP1559FeeOracleResult[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(maxFeePerGas.toString(16));
        dest.writeString(maxPriorityFeePerGas.toString(16));
        dest.writeString(name);
    }
}
