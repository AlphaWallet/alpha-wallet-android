package com.alphawallet.app.repository;

import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BAOBAB_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;

import org.web3j.protocol.http.HttpService;

public class HttpServiceHelper
{
    public static void addRequiredCredentials(long chainId, HttpService httpService, String key, boolean usesProductionKey)
    {
        if ((chainId == KLAYTN_BAOBAB_ID || chainId == KLAYTN_ID) && usesProductionKey)
        {
            httpService.addHeader("x-chain-id", Long.toString(chainId));
            httpService.addHeader("Authorization", "Basic " + key);
        }
    }
}
