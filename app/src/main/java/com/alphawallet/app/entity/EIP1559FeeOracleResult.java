package com.alphawallet.app.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.C;

import java.math.BigInteger;

/**
 * Created by JB on 20/01/2022.
 */
public class EIP1559FeeOracleResult implements Parcelable
{
    public static final BigInteger MIN_PRIORITY_FEE = BigInteger.valueOf(C.GAS_PRIORITY_FEE_MIN);

    public final BigInteger maxFeePerGas;
    public final BigInteger priorityFee;
    public final BigInteger baseFee;

    public EIP1559FeeOracleResult(BigInteger maxFee, BigInteger maxPriority, BigInteger base)
    {
        baseFee = base != null ? base : BigInteger.ZERO;
        BigInteger originalPriority = maxPriority != null ? maxPriority : BigInteger.ZERO;
        priorityFee = floorPriorityFee(originalPriority);
        // If the tip was raised to the floor, raise maxFee by the same delta so the tip remains payable.
        maxFeePerGas = floorMaxFee(maxFee, baseFee, originalPriority, priorityFee);
    }

    public EIP1559FeeOracleResult(EIP1559FeeOracleResult r)
    {
        // Re-apply the floor so Gson-loaded Realm rows with a zero tip are corrected.
        this(r.maxFeePerGas, r.priorityFee, r.baseFee);
    }

    protected EIP1559FeeOracleResult(Parcel in)
    {
        maxFeePerGas = new BigInteger(in.readString(), 16);
        priorityFee = new BigInteger(in.readString(), 16);
        baseFee = new BigInteger(in.readString(), 16);
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
        dest.writeString(priorityFee.toString(16));
        dest.writeString(baseFee.toString(16));
    }

    private static BigInteger floorPriorityFee(BigInteger input)
    {
        if (input.compareTo(BigInteger.ZERO) <= 0)
        {
            return MIN_PRIORITY_FEE;
        }
        return input.max(MIN_PRIORITY_FEE);
    }

    private static BigInteger floorMaxFee(BigInteger maxFee, BigInteger base,
                                         BigInteger originalPriority, BigInteger flooredPriority)
    {
        BigInteger tipBump = flooredPriority.subtract(originalPriority.max(BigInteger.ZERO)).max(BigInteger.ZERO);

        if (maxFee == null || maxFee.compareTo(BigInteger.ZERO) <= 0)
        {
            return base.add(flooredPriority);
        }

        return maxFee.add(tipBump).max(flooredPriority);
    }
}
