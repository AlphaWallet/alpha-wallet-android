package com.alphawallet.app.entity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JB on 21/04/2021.
 */
public class DexGuruTicker
{
    public final String address;
    public final double usdPrice;
    public final double usdChange;
    public final boolean verified;

    public DexGuruTicker(String jsonData) throws JSONException
    {
        JSONObject data = new JSONObject(jsonData);

        address = data.getString("address");
        usdPrice = data.getDouble("priceUSD");
        usdChange = data.getDouble("priceChange24h");
        verified = data.getBoolean("verified");
    }
}