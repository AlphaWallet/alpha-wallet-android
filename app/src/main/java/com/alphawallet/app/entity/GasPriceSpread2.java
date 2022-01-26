package com.alphawallet.app.entity;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.ui.widget.entity.GasSpeed;
import com.alphawallet.app.ui.widget.entity.GasSpeed2;
import com.alphawallet.app.web3.entity.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by JB on 20/01/2022.
 */
public class GasPriceSpread2 implements Parcelable
{
    public static long RAPID_SECONDS = 15;
    public static long FAST_SECONDS = 60;
    public static long STANDARD_SECONDS = 60 * 3;
    public static long SLOW_SECONDS = 60 * 10;

    public enum TXSpeed
    {
        RAPID,
        FAST,
        STANDARD,
        SLOW,
        CUSTOM
    }

    public final long timeStamp;
    public TXSpeed speedIndex = TXSpeed.STANDARD;

    public final Map<TXSpeed, GasSpeed2> fees = new HashMap<>();

    public GasPriceSpread2(Context ctx, Map<Integer, EIP1559FeeOracleResult> result)
    {
        timeStamp = System.currentTimeMillis();
        if (result == null || result.size() == 0) return;
        int third = result.size()/3;

        fees.put(TXSpeed.RAPID, new GasSpeed2(ctx.getString(R.string.speed_rapid), RAPID_SECONDS, new EIP1559FeeOracleResult(result.get(result.size()-1))));
        fees.put(TXSpeed.FAST, new GasSpeed2(ctx.getString(R.string.speed_fast), FAST_SECONDS, new EIP1559FeeOracleResult(result.get(result.get(third*2)))));
        fees.put(TXSpeed.STANDARD, new GasSpeed2(ctx.getString(R.string.speed_average), STANDARD_SECONDS, new EIP1559FeeOracleResult(result.get(result.get(third)))));
        fees.put(TXSpeed.SLOW, new GasSpeed2(ctx.getString(R.string.speed_slow), SLOW_SECONDS, new EIP1559FeeOracleResult(result.get(0))));
        fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), 0, new EIP1559FeeOracleResult(BigInteger.ZERO, BigInteger.ZERO)));
    }

    protected GasPriceSpread2(Parcel in)
    {
        timeStamp = in.readLong();
        int feeCount = in.readInt();
        int feeIndex = in.readInt();
        speedIndex = TXSpeed.values()[feeIndex];

        for (int i = 0; i < feeCount; i++)
        {
            int entry = in.readInt();
            GasSpeed2 r = in.readParcelable(GasSpeed2.class.getClassLoader());
            fees.put(TXSpeed.values()[entry], r);
        }
    }

    public static final Creator<GasPriceSpread2> CREATOR = new Creator<GasPriceSpread2>() {
        @Override
        public GasPriceSpread2 createFromParcel(Parcel in) {
            return new GasPriceSpread2(in);
        }

        @Override
        public GasPriceSpread2[] newArray(int size) {
            return new GasPriceSpread2[size];
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
        dest.writeLong(timeStamp);
        dest.writeInt(fees.size());
        dest.writeInt(speedIndex.ordinal());

        for (Map.Entry<TXSpeed, GasSpeed2> entry : fees.entrySet())
        {
            dest.writeInt(entry.getKey().ordinal());
            dest.writeParcelable(entry.getValue(), flags);
        }
    }

    public void addCustomGas(long seconds, EIP1559FeeOracleResult fee)
    {
        GasSpeed2 currentCustom = fees.get(TXSpeed.CUSTOM);
        fees.put(TXSpeed.CUSTOM,
                new GasSpeed2(currentCustom.speed, seconds, fee));
    }

    public EIP1559FeeOracleResult getCurrentGasFee()
    {
        return fees.get(this.speedIndex).gasPrice;
    }

    public long getCurrentTimeEstimate()
    {
        return fees.get(this.speedIndex).seconds;
    }

    public GasSpeed2 getGasSpeed()
    {
        return fees.get(this.speedIndex);
    }

    public boolean hasCustom()
    {
        return fees.get(TXSpeed.CUSTOM).seconds != 0;
    }
}
