package com.alphawallet.app.repository;

import android.content.Context;
import android.view.View;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.service.TickerService;

import java.util.ArrayList;
import java.util.List;

public class EthereumNetworkRepository extends EthereumNetworkBase
{
    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, TickerService tickerService, Context context)
    {
        super(preferenceRepository, tickerService, new NetworkInfo[0]);
    }

    public static void setChainColour(View view, int chainId)
    {
        view.setBackgroundResource(R.drawable.background_mainnet);
    }

    public static void setChainCircle(View view, int chainId)
    {
        view.setBackgroundResource(R.drawable.item_eth_circle);
    }

    public static List<ContractResult> getAllKnownContracts(Context context, List<Integer> chainFilters)
    {
        return EthereumNetworkBase.getAllKnownContracts(context, chainFilters);
    }

    public static List<Integer> addDefaultNetworks()
    {
        List<Integer> defaultFilter = new ArrayList<>();
        defaultFilter.add(EthereumNetworkRepository.MAINNET_ID);
        return defaultFilter;
    }

    public static String getNodeURLByNetworkId(int networkId) {
        return EthereumNetworkBase.getNodeURLByNetworkId(networkId);
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId)
    {
        return EthereumNetworkBase.getMagicLinkDomainFromNetworkId(networkId);
    }

    public static String getEtherscanURLbyNetwork(int networkId)
    {
        return EthereumNetworkBase.getEtherscanURLbyNetwork(networkId);
    }
}
