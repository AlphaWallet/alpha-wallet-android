package com.alphawallet.app.entity.lifi;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteOptions
{
    public String integrator;
    public String slippage;
    public Exchanges exchanges;
    public String order;

    public RouteOptions()
    {
        this.exchanges = new Exchanges();
    }

    public static class Exchanges
    {
        public List<String> allow = new ArrayList<>();
    }

    public JSONObject getJson() throws JSONException
    {
        String json = new Gson().toJson(this);
        return new JSONObject(json);
    }
}
