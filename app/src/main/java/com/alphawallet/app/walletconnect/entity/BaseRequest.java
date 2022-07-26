package com.alphawallet.app.walletconnect.entity;

import com.alphawallet.token.entity.Signable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public abstract class BaseRequest
{
    private static final String TAG = BaseRequest.class.getName();
    protected String rawParams;
    private final WCEthereumSignMessage.WCSignType type;
    protected List<String> params;

    public BaseRequest(String rawParams, WCEthereumSignMessage.WCSignType type)
    {
        this.rawParams = rawParams;
        this.type = type;
        Type listType = new TypeToken<List<String>>(){}.getType();
        try
        {
            params = new Gson().fromJson(rawParams, listType);
        }
        catch (Exception e)
        {
            params = Collections.emptyList();
            Timber.tag(TAG).i(rawParams);
        }
    }

    protected String getMessage()
    {
        return new WCEthereumSignMessage(params, type).getData();
    }

    public abstract Signable getSignable();

    public abstract String getWalletAddress();
}
