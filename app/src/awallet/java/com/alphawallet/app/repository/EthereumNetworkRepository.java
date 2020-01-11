package com.alphawallet.app.repository;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.service.TickerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EthereumNetworkRepository extends EthereumNetworkBase
{
    private final Context context;

    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, TickerService tickerService, Context ctx)
    {
        super(preferenceRepository, tickerService, new NetworkInfo[0], true);
        /* defaultNetwork should already have a value by now */
        if (getByName(preferences.getDefaultNetwork()) != null) {
            defaultNetwork = getByName(preferences.getDefaultNetwork());
        }
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

    public static List<Integer> addDefaultNetworks()
    {
        return new ArrayList<>(Collections.singletonList(EthereumNetworkRepository.MAINNET_ID));
    }

    public static String getNodeURLByNetworkId(int networkId) {
        return EthereumNetworkBase.getNodeURLByNetworkId(networkId);
    }

    public static String getEtherscanURLbyNetwork(int networkId)
    {
        return EthereumNetworkBase.getEtherscanURLbyNetwork(networkId);
    }

    @Override
    public List<ContractResult> getAllKnownContracts(List<Integer> networkFilters)
    {
        List<ContractResult> knownContracts = new ArrayList<>(getAllKnownContractsOnNetwork(context, EthereumNetworkRepository.MAINNET_ID, networkFilters));
        knownContracts.addAll(getAllKnownContractsOnNetwork(context, EthereumNetworkRepository.XDAI_ID, networkFilters));
        return knownContracts;
    }

    private static List<ContractResult> getAllKnownContractsOnNetwork(Context context, int chainId, List<Integer> filters)
    {
        int index = 0;

        if (!filters.contains((Integer)chainId)) return new ArrayList<>();

        List<ContractResult> result = new ArrayList<>();
        switch (chainId)
        {
            case EthereumNetworkRepository.XDAI_ID:
                index = R.array.xDAI;
                break;
            case EthereumNetworkRepository.MAINNET_ID:
                index = R.array.MainNet;
                break;
            default:
                break;
        }

        if (index > 0)
        {
            String[] strArray = context.getResources().getStringArray(index);
            for (String addr : strArray)
            {
                result.add(new ContractResult(addr, chainId));
            }
        }

        return result;
    }


    private NetworkInfo getByName(String name) {
        if (!TextUtils.isEmpty(name)) {
            for (NetworkInfo NETWORK : NETWORKS) {
                if (name.equals(NETWORK.name)) {
                    return NETWORK;
                }
            }
        }
        return null;
    }

}
