package com.alphawallet.app.walletconnect.util;

import com.alphawallet.app.walletconnect.entity.WCMethod;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class WCMethodChecker
{
    private static final List<String> methods;

    static
    {
        Gson gson = new Gson();
        String json = gson.toJson(WCMethod.values());
        Type type = new TypeToken<List<String>>()
        {
        }.getType();
        methods = gson.fromJson(json, type);
    }

    public static boolean includes(String method)
    {
        return methods.contains(method);
    }
}
