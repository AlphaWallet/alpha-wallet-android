package com.alphawallet.app.entity.ticker;

import android.text.TextUtils;

import com.alphawallet.app.entity.tokendata.TokenTicker;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class BaseTicker
{
    public final String address;
    public final double fiatPrice;
    public final BigDecimal fiatChange;

    public BaseTicker(String address, double fiatPrice, BigDecimal fiatChange)
    {
        this.address = address;
        this.fiatPrice = fiatPrice;
        this.fiatChange = fiatChange;
    }

    protected static BigDecimal getFiatChange(String fiatChangeStr)
    {
        if (TextUtils.isEmpty(fiatChangeStr)) return BigDecimal.ZERO;

        try
        {
            return new BigDecimal(fiatChangeStr);
        }
        catch (Exception e)
        {
            return BigDecimal.ZERO;
        }
    }

    public TokenTicker toTokenTicker(String currentCurrencySymbolTxt)
    {
        return new TokenTicker(String.valueOf(fiatPrice),
                fiatChange.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());
    }

    public TokenTicker toTokenTicker(String currentCurrencySymbolTxt, double conversionRate)
    {
        return new TokenTicker(String.valueOf(fiatPrice * conversionRate),
                fiatChange.setScale(3, RoundingMode.DOWN).toString(), currentCurrencySymbolTxt, "", System.currentTimeMillis());
    }
}
