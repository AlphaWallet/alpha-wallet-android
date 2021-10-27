package com.alphawallet.app.entity;

import android.content.Context;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.GasSpeed;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.alphawallet.app.util.BalanceUtils.gweiToWei;

/**
 * Created by JB on 18/11/2020.
 */
public class GasPriceSpread
{
    public static long RAPID_SECONDS = 15;
    public static long FAST_SECONDS = 60;
    public static long STANDARD_SECONDS = 60 * 3;
    public static long SLOW_SECONDS = 60 * 10;

    public final BigInteger rapid;
    public final BigInteger fast;
    public final BigInteger standard;
    public final BigInteger slow;
    public final BigInteger baseFee;
    public final boolean lockedGas;

    public final long timeStamp;

    private int customIndex;

    public GasPriceSpread(String apiReturn)
    {
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
        rapid = gweiToWei(rRapid);
        fast = gweiToWei(rFast);
        standard = gweiToWei(rStandard);
        slow = gweiToWei(rSlow);
        baseFee = gweiToWei(rBaseFee);
        lockedGas = false;
        timeStamp = System.currentTimeMillis();
    }

    public GasPriceSpread(BigInteger currentAvGasPrice, boolean hasLockedGas)
    {
        rapid = BigInteger.ZERO;
        fast = new BigDecimal(currentAvGasPrice).multiply(BigDecimal.valueOf(1.2)).toBigInteger();
        standard = currentAvGasPrice;
        slow = BigInteger.ZERO;
        baseFee = BigInteger.ZERO;
        timeStamp = System.currentTimeMillis();
        lockedGas = hasLockedGas;
    }

    public GasPriceSpread(String r, String f, String st, String sl, String bf, long timeSt, boolean locked)
    {
        rapid = new BigInteger(r);
        fast = new BigInteger(f);
        standard = new BigInteger(st);
        slow = new BigInteger(sl);
        baseFee = new BigInteger(bf);
        timeStamp = timeSt;
        lockedGas = locked;
    }

    public int setupGasSpeeds(Context ctx, List<GasSpeed> gasSpeeds, int currentGasSpeedIndex)
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

    public boolean isResultValid()
    {
        return !rapid.equals(BigInteger.ZERO) || !fast.equals(BigInteger.ZERO) || !standard.equals(BigInteger.ZERO)
                || !slow.equals(BigInteger.ZERO);
    }
}
