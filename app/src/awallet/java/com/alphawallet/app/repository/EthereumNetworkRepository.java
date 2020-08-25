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

        KnownContract knownContract = readContracts();

        if (networkFilters.contains(EthereumNetworkRepository.MAINNET_ID))
        {
            for (UnknownToken unknownToken: knownContract.getMainNet())
            {
                knownContracts.add(new ContractLocator(unknownToken.address, EthereumNetworkRepository.MAINNET_ID, !unknownToken.isPopular));
            }
        }
        if (networkFilters.contains(EthereumNetworkRepository.XDAI_ID))
        {
            for (UnknownToken unknownToken: knownContract.getXDAI())
            {
                knownContracts.add(new ContractLocator(unknownToken.address, EthereumNetworkRepository.XDAI_ID));
            }
        }
        return knownContracts;
    }

    private KnownContract readContracts()
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
