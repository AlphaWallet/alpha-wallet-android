package com.alphawallet.app.entity;

import android.text.TextUtils;

import org.bouncycastle.pqc.math.linearalgebra.CharUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JB on 21/04/2021.
 */
public class CoinGeckoTicker
{
    public final String address;
    public final double usdPrice;
    public final double usdChange;

    public CoinGeckoTicker(String address, double usdPrice, double usdChange)
    {
        this.address = address;
        this.usdChange = usdChange;
        this.usdPrice = usdPrice;
    }

    public static List<CoinGeckoTicker> buildTickerList(String jsonData) throws JSONException
    {
        List<CoinGeckoTicker> res = new ArrayList<>();
        JSONObject data = new JSONObject(jsonData);
        if (data.names() == null) return res;

        for (int i = 0; i < data.names().length(); i++)
        {
            String address = data.names().get(i).toString();
            JSONObject obj = data.getJSONObject(address);
            if (obj.has("usd"))
            {
                String usdChangeStr = obj.getString("usd_24h_change");
                double usdChange = 0.0;
                if (!TextUtils.isEmpty(usdChangeStr) && Character.isDigit(usdChangeStr.charAt(0))) usdChange = obj.getDouble("usd_24h_change");
                CoinGeckoTicker ticker = new CoinGeckoTicker(address, obj.getDouble("usd"), usdChange);
                res.add(ticker);
            }
        }

        return res;
    }
}