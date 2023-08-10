package com.alphawallet.app.ui.widget.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;
import com.alphawallet.app.util.BalanceUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by JB on 20/01/2022.
 */
public class GasSpeed implements Parcelable
{
    public final String speed;
    public long seconds;
    public final EIP1559FeeOracleResult gasPrice;

    public GasSpeed(String speed, long seconds, EIP1559FeeOracleResult gasPrice)
    {
        this.speed = speed;
        this.seconds = seconds;
        this.gasPrice = gasPrice;
    }

    public GasSpeed(Parcel in)
    {
        speed = in.readString();
        seconds = in.readLong();
        gasPrice = in.readParcelable(EIP1559FeeOracleResult.class.getClassLoader());
    }

    public GasSpeed(String speed, long seconds, BigInteger gasPrice)
    {
        this.speed = speed;
        this.seconds = seconds;
        this.gasPrice = new EIP1559FeeOracleResult(gasPrice, BigInteger.ZERO, BigInteger.ZERO);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(speed);
        dest.writeLong(seconds);
        dest.writeParcelable(gasPrice, flags);
    }

    public static final Creator<GasSpeed> CREATOR = new Creator<GasSpeed>() {
        @Override
        public GasSpeed createFromParcel(Parcel in) {
            return new GasSpeed(in);
        }

        @Override
        public GasSpeed[] newArray(int size) {
            return new GasSpeed[size];
        }
    };

    public BigDecimal calculateGasFee(BigDecimal useGasLimit, boolean isUsing1559)
    {
        if (isUsing1559)
        {
            return new BigDecimal(gasPrice.baseFee.add(gasPrice.priorityFee)).multiply(useGasLimit);
        }
        else
        {
            return new BigDecimal(gasPrice.maxFeePerGas).multiply(useGasLimit);
        }
    }

    public BigDecimal calculateMaxGasFee(BigDecimal useGasLimit)
    {
        if (gasPrice.maxFeePerGas != null && gasPrice.maxFeePerGas.compareTo(BigInteger.ZERO) > 0)
        {
            return new BigDecimal(gasPrice.maxFeePerGas).multiply(useGasLimit);
        }
        else
        {
            return BigDecimal.ZERO;
        }
    }
}
