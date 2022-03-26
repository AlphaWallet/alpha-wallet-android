package com.alphawallet.app.ui.widget.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.entity.EIP1559FeeOracleResult;

import java.math.BigInteger;

/**
 * Created by JB on 20/01/2022.
 */
public class GasSpeed2 implements Parcelable
{
    public final String speed;
    public long seconds;
    public final EIP1559FeeOracleResult gasPrice;

    public GasSpeed2(String speed, long seconds, EIP1559FeeOracleResult gasPrice)
    {
        this.speed = speed;
        this.seconds = seconds;
        this.gasPrice = gasPrice;
    }

    public GasSpeed2(Parcel in)
    {
        speed = in.readString();
        seconds = in.readLong();
        gasPrice = in.readParcelable(EIP1559FeeOracleResult.class.getClassLoader());
    }

    public GasSpeed2(String speed, long seconds, BigInteger gasPrice)
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

    public static final Creator<GasSpeed2> CREATOR = new Creator<GasSpeed2>() {
        @Override
        public GasSpeed2 createFromParcel(Parcel in) {
            return new GasSpeed2(in);
        }

        @Override
        public GasSpeed2[] newArray(int size) {
            return new GasSpeed2[size];
        }
    };
}
