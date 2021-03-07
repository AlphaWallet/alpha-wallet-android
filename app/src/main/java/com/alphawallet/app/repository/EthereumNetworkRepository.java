package com.alphawallet.app.repository;

import android.content.Context;
import android.view.View;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.KnownContract;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.UnknownToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class EthereumNetworkRepository extends EthereumNetworkBase
{
    private final Context context;
    private final HashMap<String, ContractLocator> popularTokens = new HashMap<>();

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
        return new ArrayList<>(Arrays.asList(EthereumNetworkRepository.MAINNET_ID, EthereumNetworkRepository.XDAI_ID));
    }

    public static String getNodeURLByNetworkId(int networkId) {
        return EthereumNetworkBase.getNodeURLByNetworkId(networkId);
    }

    public static String getEtherscanURLbyNetwork(int networkId)
    {
        return EthereumNetworkBase.getEtherscanURLbyNetwork(networkId);
    }

    public boolean getIsPopularToken(int chain, String address)
    {
        return popularTokens.containsKey(address.toLowerCase());
    }

    /* can't turn this one into one-liners like every other function
     * in this file, without making either EthereumNetworkBase or
     * ContractResult import android (therefore preventing their use
     * in non-Android projects) or introducing a new trivial
     * interface/class */
    public List<ContractLocator> getAllKnownContracts(List<Integer> networkFilters)
    {
        if (popularTokens.size() == 0)
        {
            buildPopularTokenMap(networkFilters);
        }

        return new ArrayList<>(popularTokens.values());
    }

    //Note: There is an issue with this method - if a contract is the same address on XDAI and MAINNET_ID it needs to be refactored
    private void buildPopularTokenMap(List<Integer> networkFilters)
    {
        KnownContract knownContract = readContracts();

        if (networkFilters == null || networkFilters.contains(EthereumNetworkRepository.MAINNET_ID))
        {
            for (UnknownToken unknownToken: knownContract.getMainNet())
            {
                popularTokens.put(unknownToken.address.toLowerCase(), new ContractLocator(unknownToken.address, EthereumNetworkRepository.MAINNET_ID));
            }
        }
        if (networkFilters == null || networkFilters.contains(EthereumNetworkRepository.XDAI_ID))
        {
            for (UnknownToken unknownToken: knownContract.getXDAI())
            {
                popularTokens.put(unknownToken.address.toLowerCase(), new ContractLocator(unknownToken.address, EthereumNetworkRepository.XDAI_ID));
            }
        }
    }

    @Override
    public KnownContract readContracts()
    {
        String jsonString;
        try
        {
            InputStream is = context.getAssets().open("known_contract.json");

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, "UTF-8");
        }
        catch (IOException e) {
            return null;
        }

        return new Gson().fromJson(jsonString, KnownContract.class);
    }
}
