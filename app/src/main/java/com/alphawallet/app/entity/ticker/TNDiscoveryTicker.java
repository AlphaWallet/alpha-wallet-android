package com.alphawallet.app.entity.ticker;


import com.alphawallet.app.entity.tokendata.TokenTicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

public class TNDiscoveryTicker extends BaseTicker
{
    public TNDiscoveryTicker(String address, double fiatPrice, BigDecimal fiatChange)
    {
        super(address, fiatPrice, fiatChange);
    }

    public TNDiscoveryTicker(JSONObject result, String address) throws JSONException
    {
        super(address, result.getDouble("usdPrice"), getFiatChange(result.getString("24hrPercentChange")));
    }

    public static void toTokenTickers(Map<String, TokenTicker> tickers, JSONArray result, String currentCurrencySymbolTxt, double currentConversionRate) throws JSONException
    {
        for (int i = 0; i < result.length(); i++)
        {
            JSONObject thisTickerObject = result.getJSONObject(i);
            TNDiscoveryTicker thisTicker = new TNDiscoveryTicker(thisTickerObject, thisTickerObject.getString("contract"));
            tickers.put(thisTickerObject.getString("contract"), thisTicker.toTokenTicker(currentCurrencySymbolTxt, currentConversionRate));
        }
    }
}
