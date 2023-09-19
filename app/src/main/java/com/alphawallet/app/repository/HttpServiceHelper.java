package com.alphawallet.app.repository;

import static com.alphawallet.app.repository.EthereumNetworkBase.INFURA_DOMAIN;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BAOBAB_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;

import android.text.TextUtils;

import org.web3j.protocol.http.HttpService;

import okhttp3.Request;

public class HttpServiceHelper
{
    public static void addRequiredCredentials(long chainId, HttpService httpService, String klaytnKey, String infuraKey, boolean usesProductionKey)
    {
        String serviceUrl = httpService.getUrl();
        if ((chainId == KLAYTN_BAOBAB_ID || chainId == KLAYTN_ID) && usesProductionKey)
        {
            httpService.addHeader("x-chain-id", Long.toString(chainId));
            httpService.addHeader("Authorization", "Basic " + klaytnKey);
        }
        else if (serviceUrl != null && usesProductionKey && serviceUrl.contains(INFURA_DOMAIN) && !TextUtils.isEmpty(infuraKey))
        {
            httpService.addHeader("Authorization", "Basic " + infuraKey);
        }
    }

    public static void addRequiredCredentials(long chainId, Request.Builder service, String klaytnKey, String infuraKey, boolean usesProductionKey, boolean isInfura)
    {
        if ((chainId == KLAYTN_BAOBAB_ID || chainId == KLAYTN_ID) && usesProductionKey)
        {
            service.addHeader("x-chain-id", Long.toString(chainId));
            service.addHeader("Authorization", "Basic " + klaytnKey);
        }
        else if (isInfura && usesProductionKey && !TextUtils.isEmpty(infuraKey))
        {
            service.addHeader("Authorization", "Basic " + infuraKey);
        }
    }
}
