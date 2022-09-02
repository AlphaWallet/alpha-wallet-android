package com.alphawallet.app.repository;

import android.content.Context;

import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.KnownContract;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.UnknownToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;

public class EthereumNetworkRepository extends EthereumNetworkBase
{
    private final Context context;
    private final HashMap<String, ContractLocator> popularTokens = new HashMap<>();

    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, Context ctx)
    {
        super(preferenceRepository, new NetworkInfo[0], true);
        context = ctx;
    }

    public static String getNodeURLByNetworkId(long networkId) {
        return EthereumNetworkBase.getNodeURLByNetworkId(networkId);
    }

    public boolean getIsPopularToken(long chainId, String address)
    {
        return popularTokens.containsKey(address.toLowerCase());
    }

    /* can't turn this one into one-liners like every other function
     * in this file, without making either EthereumNetworkBase or
     * ContractResult import android (therefore preventing their use
     * in non-Android projects) or introducing a new trivial
     * interface/class */
    public List<ContractLocator> getAllKnownContracts(List<Long> networkFilters)
    {
        if (popularTokens.size() == 0)
        {
            buildPopularTokenMap(networkFilters);
        }

        return new ArrayList<>(popularTokens.values());
    }

    //Note: There is an issue with this method - if a contract is the same address on XDAI and MAINNET_ID it needs to be refactored
    private void buildPopularTokenMap(List<Long> networkFilters)
    {
        KnownContract knownContract = readContracts();
        if (knownContract == null) return;

        if (networkFilters == null || networkFilters.contains(MAINNET_ID))
        {
            for (UnknownToken unknownToken: knownContract.getMainNet())
            {
                popularTokens.put(unknownToken.address.toLowerCase(), new ContractLocator(unknownToken.address, MAINNET_ID));
            }
        }
        if (networkFilters == null || networkFilters.contains(GNOSIS_ID))
        {
            for (UnknownToken unknownToken: knownContract.getXDAI())
            {
                popularTokens.put(unknownToken.address.toLowerCase(), new ContractLocator(unknownToken.address, GNOSIS_ID));
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

            jsonString = new String(buffer, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            return null;
        }

        return new Gson().fromJson(jsonString, KnownContract.class);
    }
}
