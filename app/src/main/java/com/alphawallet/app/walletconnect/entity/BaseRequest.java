package com.alphawallet.app.walletconnect.entity;

import static java.util.Arrays.asList;

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
        Timber.tag(TAG).i(rawParams);

        this.rawParams = rawParams;
        this.type = type;
        try
        {
            params = new Gson().fromJson(rawParams, new TypeToken<List<String>>()
            {
            }.getType());
        }
        catch (Exception e)
        {
            String unwrapped = unwrap();
            int index = unwrapped.indexOf(", ");
            params = asList(unwrapped.substring(0, index), unwrapped.substring(index + 2));
        }
    }

    protected String unwrap()
    {
        StringBuilder stringBuilder = new StringBuilder(rawParams);
        return stringBuilder.substring(1, stringBuilder.length() - 1);
    }

    protected String getMessage()
    {
        return new WCEthereumSignMessage(params, type).getData();
    }

    public abstract Signable getSignable();

    public abstract String getWalletAddress();
}
