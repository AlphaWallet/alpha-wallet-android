package com.alphawallet.app.entity;

import android.text.TextUtils;

import com.alphawallet.app.entity.tokendata.TokenTicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by JB on 21/04/2021.
 */
public class CoinGeckoTicker
{
    public final String address;
    public final double fiatPrice;
    public final BigDecimal fiatChange;

    public CoinGeckoTicker(String address, double fiatPrice, BigDecimal fiatChange)
    {
        this.address = address;
        this.fiatChange = fiatChange;
        this.fiatPrice = fiatPrice;
    }

    public static List<CoinGeckoTicker> buildTickerList(String jsonData, String currencyIsoSymbol, double currentConversionRate) throws JSONException
    {
        List<CoinGeckoTicker> res = new ArrayList<>();
        JSONObject data = new JSONObject(jsonData);
        if (data.names() == null) return res;

        for (int i = 0; i < data.names().length(); i++)
        {
            String address = data.names().get(i).toString();
            JSONObject obj = data.getJSONObject(address);
            double fiatPrice = 0.0;
            String fiatChangeStr;
            if (obj.has(currencyIsoSymbol.toLowerCase()))
            {
                fiatPrice = obj.getDouble(currencyIsoSymbol.toLowerCase());
                fiatChangeStr = obj.getString(currencyIsoSymbol.toLowerCase() + "_24h_change");
            }
            else
            {
                fiatPrice = obj.getDouble("usd") * currentConversionRate;
                fiatChangeStr = obj.getString("usd_24h_change");
            }

            res.add(new CoinGeckoTicker(address, fiatPrice, getFiatChange(fiatChangeStr)));
        }

        return res;
    }

    private static BigDecimal getFiatChange(String fiatChangeStr)
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
}