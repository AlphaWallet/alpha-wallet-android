package com.alphawallet.app.repository;

import android.content.Context;
import android.view.View;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.NetworkInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EthereumNetworkRepository extends EthereumNetworkBase
{
    private final Context context;

    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, Context ctx)
    {
        super(preferenceRepository, new NetworkInfo[0], true);
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

    /* can't turn this one into one-liners like every other function
     * in this file, without making either EthereumNetworkBase or
     * ContractResult import android (therefore preventing their use
     * in non-Android projects) or introducing a new trivial
     * interface/class */
    public List<ContractLocator> getAllKnownContracts(List<Integer> networkFilters)
    {
        List<ContractLocator> knownContracts = new ArrayList<>();
        if (networkFilters.contains(EthereumNetworkRepository.MAINNET_ID)) {
            knownContracts.addAll(Arrays.asList(ContractLocator.fromAddresses(context.getResources().getStringArray(R.array.MainNet), EthereumNetworkRepository.MAINNET_ID)));
        }
        if (networkFilters.contains(EthereumNetworkRepository.XDAI_ID)) {
            knownContracts.addAll(Arrays.asList(ContractLocator.fromAddresses(context.getResources().getStringArray(R.array.xDAI), EthereumNetworkRepository.XDAI_ID)));
        }
        return knownContracts;
    }
}
