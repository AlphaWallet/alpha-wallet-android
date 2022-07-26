package com.alphawallet.app.walletconnect.entity;

import com.alphawallet.token.entity.Signable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public abstract class BaseRequest
{
    protected String rawParams;
    private final WCEthereumSignMessage.WCSignType type;
    protected List<String> params;

    public BaseRequest(String rawParams, WCEthereumSignMessage.WCSignType type)
    {
        this.rawParams = rawParams;
        this.type = type;
        Type listType = new TypeToken<List<String>>(){}.getType();
        params = new Gson().fromJson(rawParams, listType); // Once WC team fixed the params format, can use this
    }

    protected String getMessage()
    {
        return new WCEthereumSignMessage(params, type).getData();
    }

    public abstract Signable getSignable();

    public abstract String getWalletAddress();
}
