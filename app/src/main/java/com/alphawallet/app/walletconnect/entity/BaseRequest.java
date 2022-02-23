package com.alphawallet.app.walletconnect.entity;

import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;

import java.util.List;

import static java.util.Arrays.asList;

public abstract class BaseRequest
{
    protected String rawParams;
    private WCEthereumSignMessage.WCSignType type;
    protected List<String> params;

    public BaseRequest(String rawParams, WCEthereumSignMessage.WCSignType type)
    {
        this.rawParams = rawParams;
        this.type = type;
        params = asList(unwrap().split(", "));
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
