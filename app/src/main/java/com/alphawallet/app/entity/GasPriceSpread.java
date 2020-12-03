package com.alphawallet.app.entity;

import android.content.Context;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.GasSpeed;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.List;

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

    public final long timeStamp;

    public GasPriceSpread(String apiReturn)
    {
        BigInteger rRapid = BigInteger.ZERO;
        BigInteger rFast = BigInteger.ZERO;
        BigInteger rStandard = BigInteger.ZERO;
        BigInteger rSlow = BigInteger.ZERO;
        long rTimeStamp = 0;

        try
        {
            JSONObject result = new JSONObject(apiReturn);
            JSONObject data = result.getJSONObject("data");
            rRapid = new BigInteger(data.getString("rapid"));
            rFast = new BigInteger(data.getString("fast"));
            rStandard = new BigInteger(data.getString("standard"));
            rSlow = new BigInteger(data.getString("slow"));
            rTimeStamp = data.getLong("timeStamp");
        }
        catch (JSONException e)
        {
            //
        }

        rapid = rRapid;
        fast = rFast;
        standard = rStandard;
        slow = rSlow;
        timeStamp = rTimeStamp;
    }

    public GasPriceSpread(BigInteger currentAvGasPrice)
    {
        rapid = BigInteger.ZERO;
        fast = currentAvGasPrice;
        standard = BigInteger.ZERO;
        slow = BigInteger.ZERO;
        timeStamp = System.currentTimeMillis();
    }

    public GasPriceSpread(String r, String f, String st, String sl, long timeSt)
    {
        rapid = new BigInteger(r);
        fast = new BigInteger(f);
        standard = new BigInteger(st);
        slow = new BigInteger(sl);
        timeStamp = timeSt;
    }

    public int setupGasSpeeds(Context ctx, List<GasSpeed> gasSpeeds, int currentGasSpeedIndex)
    {
        GasSpeed customGas = null;
        if (gasSpeeds.size() > 0) customGas = gasSpeeds.get(gasSpeeds.size()-1);

        gasSpeeds.clear();

        if (rapid.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_rapid), GasPriceSpread.RAPID_SECONDS, rapid));
        }

        if (fast.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_fast), GasPriceSpread.FAST_SECONDS, fast));
        }

        if (standard.compareTo(BigInteger.ZERO) > 0)
        {
            if (currentGasSpeedIndex == -1) currentGasSpeedIndex = gasSpeeds.size();
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_average), GasPriceSpread.STANDARD_SECONDS, standard));
        }

        if (slow.compareTo(BigInteger.ZERO) > 0)
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_slow), GasPriceSpread.SLOW_SECONDS, slow));
        }

        if (customGas != null)
        {
            gasSpeeds.add(customGas);
        }
        else
        {
            gasSpeeds.add(new GasSpeed(ctx.getString(R.string.speed_custom), 0, BigInteger.ZERO));
        }

        if (currentGasSpeedIndex < 0 || currentGasSpeedIndex >= gasSpeeds.size())
        {
            currentGasSpeedIndex = 0;
        }

        return currentGasSpeedIndex;
    }
}
