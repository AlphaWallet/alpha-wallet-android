package com.alphawallet.app.repository;

import static com.alphawallet.app.repository.EthereumNetworkBase.INFURA_DOMAIN;

import android.text.TextUtils;

import org.web3j.protocol.http.HttpService;

import okhttp3.Request;

public class HttpServiceHelper
{
    public static void addRequiredCredentials(String serviceUrl, HttpService httpService, String infuraKey)
    {
        if (serviceUrl != null && serviceUrl.contains(INFURA_DOMAIN) && !TextUtils.isEmpty(infuraKey))
        {
            httpService.addHeader("Authorization", "Basic " + infuraKey);
        }
    }

    public static void addRequiredCredentials(String serviceUrl, Request.Builder service, String infuraKey)
    {
        if (serviceUrl != null && serviceUrl.contains(INFURA_DOMAIN) && !TextUtils.isEmpty(infuraKey))
        {
            service.addHeader("Authorization", "Basic " + infuraKey);
        }
    }

    public static void addInfuraGasCredentials(Request.Builder service, String infuraSecret)
    {
        service.addHeader("Authorization", "Basic " + infuraSecret);
    }
}
