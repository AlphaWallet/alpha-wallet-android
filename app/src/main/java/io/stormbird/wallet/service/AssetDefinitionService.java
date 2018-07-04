package io.stormbird.wallet.service;


import android.content.Context;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;


/**
 * AssetDefinitionService is the only place where we interface with XML files.
 * This is to avoid duplicating resources (previously the XML was loaded in several places)
 * and also provide a consistent way to get XML values
 */
public class AssetDefinitionService
{
    private TokenDefinition assetDefinition;
    private NetworkInfo currentNetworkInfo;
    private Context context;

    public AssetDefinitionService(Context ctx)
    {
        init(ctx);
    }

    private void init(Context ctx)
    {
        context = ctx;
        assetDefinition = null;
        try
        {
            //TODO: Multi-XML handling
            assetDefinition = new TokenDefinition(
                    ctx.getResources().getAssets().open("TicketingContract.xml"),
                    ctx.getResources().getConfiguration().locale);

        }
        catch (IOException | SAXException e)
        {
            e.printStackTrace();
        }
    }

    //TODO: these won't be needed when we have multi-XML handling
    public void setCurrentNetwork(NetworkInfo networkInfo)
    {
        currentNetworkInfo = networkInfo;
    }
    private boolean isMainNet()
    {
        return currentNetworkInfo.isMainNetwork;
    }

    public TokenDefinition getAssetDefinition()
    {
        return assetDefinition;
    }

    public NonFungibleToken getNonFungibleToken(BigInteger v)
    {
        return new NonFungibleToken(v, assetDefinition);
    }

    /**
     * TODO: Function should scan all XML files and extract the contract addresses
     * @param networkId
     * @return
     */
    public List<String> getAllContracts(int networkId)
    {
        List<String> contractList = new ArrayList<>();
        contractList.add(assetDefinition.getContractAddress(networkId));
        return contractList;
    }

    public String getIssuerName(String contractAddress)
    {
        //only specify the issuer name if we're on mainnet, otherwise default to 'Ethereum'
        //TODO: Remove the main-net stipulation once we do multi-XML handling
        if (isMainNet() && assetDefinition.getNetworkFromContract(contractAddress) > 0)
        {
            return assetDefinition.getKeyName();
        }
        else
        {
            return context.getString(R.string.ethereum);
        }
    }
}
