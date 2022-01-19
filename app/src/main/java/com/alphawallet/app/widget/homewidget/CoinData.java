package com.alphawallet.app.widget.homewidget;

import org.json.JSONObject;

public class CoinData
{
    public String name;
    public String symbol;
    public float price;
    public float change_1h;
    public float change_24h;
    public float change_7d;
    public int   rank;

    public CoinData(String coinName, String coinCode)
    {
        name = coinName;
        symbol = coinCode;
    }

    public CoinData(JSONObject stateData, String currency, Double rate)
    {
        try
        {
            String price_in_currency = "currentPrice";
            name = stateData.getString("name");
            symbol = stateData.getString("symbol");

            change_1h = Float.parseFloat(stateData.getString("changeInPriceHourly"));
            change_24h = Float.parseFloat(stateData.getString("changeInPriceDaily"));
            change_7d = Float.parseFloat(stateData.getString("changeInPriceWeekly"));
            rank = Integer.parseInt(stateData.getString("rank"));

            price = Float.parseFloat(stateData.getString("currentPrice")) * rate.floatValue();
        }
        catch (org.json.JSONException | NumberFormatException e)
        {
            e.printStackTrace();
        }
    }

    /*
    {"id":1,"name":"Bitcoin","symbol":"BTC","slug":"bitcoin","rank":1,"is_active":1,"first_historical_data":"2013-04-28T18:47:21.000Z","last_historical_data":"2021-06-07T01:29:03.000Z","platform":null}
     */
    public CoinData(JSONObject stateData)
    {
        try
        {
            name = stateData.getString("name");
            symbol = stateData.getString("symbol");
            rank = Integer.parseInt(stateData.getString("rank"));
        }
        catch (org.json.JSONException | NumberFormatException e)
        {
            e.printStackTrace();
        }
    }
}