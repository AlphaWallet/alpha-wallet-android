package com.alphawallet.app.repository;

import android.content.Context;
import android.view.View;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.service.TickerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EthereumNetworkRepository extends EthereumNetworkBase
{
    private final Context context;
    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, TickerService tickerService, Context ctx)
    {
        super(preferenceRepository, tickerService, new NetworkInfo[0], true);
        context = ctx;
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
        return new ArrayList<>(Collections.singletonList(EthereumNetworkRepository.MAINNET_ID));
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

    @Override
    public List<ContractResult> getAllKnownContracts(List<Integer> networkFilters)
    {
        return EthereumNetworkBase.getAllKnownContracts(context, networkFilters);
    }
}
