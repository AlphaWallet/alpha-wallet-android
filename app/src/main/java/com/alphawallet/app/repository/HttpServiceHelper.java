package com.alphawallet.app.repository;

import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BOABAB_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;

import org.web3j.protocol.http.HttpService;

public class HttpServiceHelper
{
    public static void addRequiredCredentials(long chainId, HttpService httpService, String key)
    {
        if ((chainId == KLAYTN_BOABAB_ID || chainId == KLAYTN_ID) && EthereumNetworkBase.usesProductionKey)
        {
            httpService.addHeader("x-chain-id", Long.toString(chainId));
            httpService.addHeader("Authorization", "Basic " + key);
        }
    }
}
