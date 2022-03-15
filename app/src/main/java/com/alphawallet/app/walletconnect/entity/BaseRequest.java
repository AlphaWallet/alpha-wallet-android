package com.alphawallet.app.walletconnect.entity;

import com.alphawallet.token.entity.Signable;

import java.util.List;

import static java.util.Arrays.asList;

public abstract class BaseRequest
{
    protected String rawParams;
    private final WCEthereumSignMessage.WCSignType type;
    protected List<String> params;

    public BaseRequest(String rawParams, WCEthereumSignMessage.WCSignType type)
    {
        this.rawParams = rawParams;
        this.type = type;
//        Type listType = new TypeToken<List<String>>(){}.getType();
//        params = new Gson().fromJson(rawParams, listType); // Once WC team fixed the params format, can use this
        String unwrapped = unwrap();
        int index = unwrapped.indexOf(", ");
        params = asList(unwrapped.substring(0, index), unwrapped.substring(index + 2));
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
