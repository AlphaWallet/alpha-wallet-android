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

    public final List<EIP1559FeeOracleResult> fees = new ArrayList<>();

    public GasPriceSpread2(Map<Integer, EIP1559FeeOracleResult> result, Context ctx)
    {
        timeStamp = System.currentTimeMillis();
        if (result == null || result.size() == 0) return;
        int third = result.size()/3;

        fees.add(new EIP1559FeeOracleResult(result.get(result.size()-1), ctx.getString(R.string.speed_rapid)));
        fees.add(new EIP1559FeeOracleResult(result.get(result.get(third*2)), ctx.getString(R.string.speed_fast)));
        fees.add(new EIP1559FeeOracleResult(result.get(result.get(third)), ctx.getString(R.string.speed_average)));
        fees.add(new EIP1559FeeOracleResult(result.get(result.get(0)), ctx.getString(R.string.speed_slow)));
    }

    protected GasPriceSpread2(Parcel in)
    {
        timeStamp = in.readLong();
        int feeCount = in.readInt();
        int feeIndex = in.readInt();
        speedIndex = TXSpeed.values()[feeIndex];

        for (int i = 0; i < feeCount; i++)
        {
            EIP1559FeeOracleResult r = in.readParcelable(EIP1559FeeOracleResult.class.getClassLoader());
            fees.add(r);
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
        for (EIP1559FeeOracleResult r : fees)
        {
            dest.writeParcelable(r, flags);
        }
    }

    public int setupGasSpeeds(Context ctx, int currentGasSpeedIndex)
    {
        boolean hasRapid = false;
        GasSpeed customGas = null;
        if (gasSpeeds.size() > 0) customGas = gasSpeeds.get(gasSpeeds.size()-1);

        gasSpeeds.clear();

        if (rapid.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_rapid), GasPriceSpread.RAPID_SECONDS, rapid));
            hasRapid = true;
        }

        if (fast.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_fast), GasPriceSpread.FAST_SECONDS, fast));
            hasRapid = true;
        }

        if (standard.compareTo(BigInteger.ZERO) > 0)
        {
            long txTime = GasPriceSpread.STANDARD_SECONDS;
            if (currentGasSpeedIndex == -1) currentGasSpeedIndex = gasSpeeds.size();
            if (!hasRapid) txTime = GasPriceSpread.FAST_SECONDS; //for non mainnet chains, assume standard tx time is 1 minute
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_average), txTime, standard));
        }

        if (slow.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_slow), GasPriceSpread.SLOW_SECONDS, slow));
        }

        customIndex = gasSpeeds.size();
        if (customGas != null)
        {
            gasSpeeds.add(customGas);
        }
        else
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_custom), 0, BigInteger.ZERO, true));
        }

        if (currentGasSpeedIndex < 0 || currentGasSpeedIndex >= gasSpeeds.size())
        {
            currentGasSpeedIndex = 0;
        }

        return currentGasSpeedIndex;
    }

    public int getCustomIndex()
    {
        return customIndex;
    }
}
