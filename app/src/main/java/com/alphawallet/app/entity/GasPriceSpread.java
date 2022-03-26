package com.alphawallet.app.entity;

import static com.alphawallet.app.util.BalanceUtils.gweiToWei;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.GasSpeed2;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 20/01/2022.
 */
public class GasPriceSpread implements Parcelable
{
    public static long RAPID_SECONDS = 15;
    public static long FAST_SECONDS = 60;
    public static long STANDARD_SECONDS = 60 * 3;
    public static long SLOW_SECONDS = 60 * 10;

    private final boolean hasLockedGas;
    private BigInteger baseFee = BigInteger.ZERO;

    public GasSpeed2 getSelectedGasFee(TXSpeed currentGasSpeedIndex)
    {
        return fees.get(currentGasSpeedIndex);
    }

    public int getEntrySize()
    {
        return fees.size();
    }

    public GasSpeed2 getQuickestGasSpeed()
    {
        for (TXSpeed txs : TXSpeed.values())
        {
            GasSpeed2 gs = fees.get(txs);
            if (gs != null) return gs;
        }

        //Should not reach this
        return null;
    }

    public GasSpeed2 getSlowestGasSpeed()
    {
        TXSpeed slowest = TXSpeed.STANDARD;
        for (TXSpeed txs : TXSpeed.values())
        {
            GasSpeed2 gs = fees.get(txs);
            if (gs != null)
            {
                if (gs.gasPrice.maxFeePerGas.compareTo(fees.get(slowest).gasPrice.maxFeePerGas) < 0)
                {
                    slowest = txs;
                }
            }
        }

        return fees.get(slowest);
    }

    public TXSpeed getNextSpeed(TXSpeed speed)
    {
        boolean begin = false;
        for (TXSpeed txs : TXSpeed.values())
        {
            GasSpeed2 gs = fees.get(txs);
            if (gs != null)
            {
                if (txs == speed)
                {
                    begin = true;
                }
                else if (begin)
                {
                    return txs;
                }
            }
        }

        return TXSpeed.CUSTOM;
    }

    public TXSpeed getSelectedPosition(int absoluteAdapterPosition)
    {
        int index = 0;
        for (TXSpeed txs : TXSpeed.values())
        {
            GasSpeed2 gs = fees.get(txs);
            if (gs == null) continue;
            else if (absoluteAdapterPosition == index) return txs;
            index++;
        }

        return TXSpeed.CUSTOM;
    }

    public final long timeStamp;
    public TXSpeed speedIndex = TXSpeed.STANDARD;

    private final Map<TXSpeed, GasSpeed2> fees = new HashMap<>();

    public GasPriceSpread(Context ctx, Map<Integer, EIP1559FeeOracleResult> result)
    {
        hasLockedGas = false;
        timeStamp = System.currentTimeMillis();
        if (result == null || result.size() == 0) return;
        setComponents(ctx, result);

        fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), STANDARD_SECONDS, fees.get(TXSpeed.STANDARD).gasPrice));
    }

    public GasPriceSpread(Context ctx, GasPriceSpread gs, Map<Integer, EIP1559FeeOracleResult> result)
    {
        hasLockedGas = false;
        timeStamp = System.currentTimeMillis();
        if (result == null || result.size() == 0) return;
        setComponents(ctx, result);

        GasSpeed2 custom = gs.getSelectedGasFee(TXSpeed.CUSTOM);

        fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), custom.seconds, custom.gasPrice));
    }

    //This is a fallback method, it should never be used
    public GasPriceSpread(Context ctx, BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas)
    {
        timeStamp = System.currentTimeMillis();

        BigInteger baseFeeApprox = maxFeePerGas.subtract(maxPriorityFeePerGas.divide(BigInteger.valueOf(2)));

        fees.put(TXSpeed.STANDARD, new GasSpeed2(ctx.getString(R.string.speed_average), STANDARD_SECONDS, new EIP1559FeeOracleResult(maxFeePerGas, maxPriorityFeePerGas, baseFeeApprox)));
        fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), STANDARD_SECONDS, new EIP1559FeeOracleResult(maxFeePerGas, maxPriorityFeePerGas, baseFeeApprox)));
        hasLockedGas = false;
    }

    public GasPriceSpread(BigInteger currentAvGasPrice, boolean lockedGas)
    {
        timeStamp = System.currentTimeMillis();

        fees.put(TXSpeed.FAST, new GasSpeed2("", FAST_SECONDS, new BigDecimal(currentAvGasPrice).multiply(BigDecimal.valueOf(1.2)).toBigInteger()));
        fees.put(TXSpeed.STANDARD, new GasSpeed2("", STANDARD_SECONDS, currentAvGasPrice));
        hasLockedGas = lockedGas;
    }

    public GasPriceSpread(Context ctx, BigInteger gasPrice)
    {
        timeStamp = System.currentTimeMillis();

        fees.put(TXSpeed.STANDARD, new GasSpeed2(ctx.getString(R.string.speed_average), STANDARD_SECONDS, gasPrice));
        fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), STANDARD_SECONDS, gasPrice));
        hasLockedGas = false;
    }

    public GasPriceSpread(String apiReturn)
    {
        this.timeStamp = System.currentTimeMillis();

        BigDecimal rRapid = BigDecimal.ZERO;
        BigDecimal rFast = BigDecimal.ZERO;
        BigDecimal rStandard = BigDecimal.ZERO;
        BigDecimal rSlow = BigDecimal.ZERO;
        BigDecimal rBaseFee = BigDecimal.ZERO;

        try
        {
            JSONObject result = new JSONObject(apiReturn);
            if (result.has("result"))
            {
                JSONObject data = result.getJSONObject("result");

                rFast = new BigDecimal(data.getString("FastGasPrice"));
                rRapid = rFast.multiply(BigDecimal.valueOf(1.2));
                rStandard = new BigDecimal(data.getString("ProposeGasPrice"));
                rSlow = new BigDecimal(data.getString("SafeGasPrice"));
                rBaseFee = new BigDecimal(data.getString("suggestBaseFee"));
            }
        }
        catch (JSONException e)
        {
            //
        }

        //convert to wei
        fees.put(TXSpeed.RAPID, new GasSpeed2("", RAPID_SECONDS, gweiToWei(rRapid)));
        fees.put(TXSpeed.FAST, new GasSpeed2("", FAST_SECONDS, gweiToWei(rFast)));
        fees.put(TXSpeed.STANDARD, new GasSpeed2("", STANDARD_SECONDS, gweiToWei(rStandard)));
        fees.put(TXSpeed.SLOW, new GasSpeed2("", SLOW_SECONDS, gweiToWei(rSlow)));
        baseFee = gweiToWei(rBaseFee);

        hasLockedGas = false;
    }

    public GasPriceSpread(Context ctx, GasPriceSpread gasSpread, long timestamp, Map<TXSpeed, BigInteger> feeMap, boolean locked)
    {
        this.timeStamp = timestamp;

        addLegacyGasFee(TXSpeed.RAPID, RAPID_SECONDS, ctx.getString(R.string.speed_rapid), feeMap);
        addLegacyGasFee(TXSpeed.FAST, FAST_SECONDS, ctx.getString(R.string.speed_fast), feeMap);
        addLegacyGasFee(TXSpeed.STANDARD, STANDARD_SECONDS, ctx.getString(R.string.speed_average), feeMap);
        addLegacyGasFee(TXSpeed.SLOW, SLOW_SECONDS, ctx.getString(R.string.speed_slow), feeMap);

        if (gasSpread != null)
        {
            GasSpeed2 custom = gasSpread.getSelectedGasFee(TXSpeed.CUSTOM);

            if (custom != null)
            {
                fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), custom.seconds, custom.gasPrice.maxFeePerGas));
            }
        }
        else
        {
            fees.put(TXSpeed.CUSTOM, new GasSpeed2(ctx.getString(R.string.speed_custom), STANDARD_SECONDS, feeMap.get(TXSpeed.STANDARD)));
        }
        hasLockedGas = locked;
    }

    private void addLegacyGasFee(TXSpeed speed, long seconds, String speedName, Map<TXSpeed, BigInteger> feeMap)
    {
        BigInteger gasPrice = feeMap.get(speed);
        if (gasPrice != null && gasPrice.compareTo(BigInteger.ZERO) > 0)
        {
            fees.put(speed, new GasSpeed2(speedName, seconds, gasPrice));
        }
    }

    private void setComponents(Context ctx, Map<Integer, EIP1559FeeOracleResult> result)
    {
        int third = result.size()/3;

        fees.put(TXSpeed.RAPID, new GasSpeed2(ctx.getString(R.string.speed_rapid), RAPID_SECONDS, new EIP1559FeeOracleResult(result.get(0))));
        fees.put(TXSpeed.FAST, new GasSpeed2(ctx.getString(R.string.speed_fast), FAST_SECONDS, new EIP1559FeeOracleResult(result.get(third))));
        fees.put(TXSpeed.STANDARD, new GasSpeed2(ctx.getString(R.string.speed_average), STANDARD_SECONDS, new EIP1559FeeOracleResult(result.get(third*2))));
        fees.put(TXSpeed.SLOW, new GasSpeed2(ctx.getString(R.string.speed_slow), SLOW_SECONDS, new EIP1559FeeOracleResult(result.get(result.size()-1))));

        //now de-duplicate
        for (TXSpeed txs : TXSpeed.values())
        {
            GasSpeed2 gs = fees.get(txs);
            if (txs == TXSpeed.STANDARD || gs == null) continue;
            if (gs.gasPrice.maxPriorityFeePerGas.equals(fees.get(TXSpeed.STANDARD).gasPrice.maxPriorityFeePerGas)
                && gs.gasPrice.maxFeePerGas.equals(fees.get(TXSpeed.STANDARD).gasPrice.maxFeePerGas))
            {
                fees.remove(txs);
            }
        }
    }

    public void setCustom(BigInteger maxFeePerGas, BigInteger maxPriorityFeePerGas, long fastSeconds)
    {
        GasSpeed2 gsCustom = fees.get(TXSpeed.CUSTOM);
        BigInteger baseFee = gsCustom.gasPrice.baseFee;
        fees.put(TXSpeed.CUSTOM, new GasSpeed2(gsCustom.speed, fastSeconds, new EIP1559FeeOracleResult(maxFeePerGas, maxPriorityFeePerGas, baseFee)));
    }

    public void setCustom(BigInteger gasPrice, long fastSeconds)
    {
        GasSpeed2 gsCustom = fees.get(TXSpeed.CUSTOM);
        fees.put(TXSpeed.CUSTOM, new GasSpeed2(gsCustom.speed, fastSeconds, gasPrice));
    }

    protected GasPriceSpread(Parcel in)
    {
        timeStamp = in.readLong();
        int feeCount = in.readInt();
        int feeIndex = in.readInt();
        speedIndex = TXSpeed.values()[feeIndex];
        hasLockedGas = in.readByte() == 1;

        for (int i = 0; i < feeCount; i++)
        {
            int entry = in.readInt();
            GasSpeed2 r = in.readParcelable(GasSpeed2.class.getClassLoader());
            fees.put(TXSpeed.values()[entry], r);
        }
    }

    public static final Creator<GasPriceSpread> CREATOR = new Creator<GasPriceSpread>() {
        @Override
        public GasPriceSpread createFromParcel(Parcel in) {
            return new GasPriceSpread(in);
        }

        @Override
        public GasPriceSpread[] newArray(int size) {
            return new GasPriceSpread[size];
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
        dest.writeByte(hasLockedGas ? (byte) 1 : (byte) 0);

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

    public boolean hasLockedGas() { return hasLockedGas; }

    public BigInteger getBaseFee() { return baseFee; }

    public boolean isResultValid()
    {
        for (TXSpeed txs : TXSpeed.values())
        {
            GasSpeed2 gs = fees.get(txs);
            if (gs != null && gs.gasPrice.maxFeePerGas.compareTo(BigInteger.ZERO) > 0) return true;
        }

        return false;
    }

    public int findItem(TXSpeed currentGasSpeedIndex)
    {
        int index = 0;
        for (TXSpeed txs : TXSpeed.values())
        {
            if (txs == currentGasSpeedIndex) return index;
            if (fees.get(txs) != null) index++;
        }

        return 0;
    }
}
