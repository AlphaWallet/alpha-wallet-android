package com.alphawallet.app.di.mock;

import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.KeyProvider;

public class KeyProviderMockNonProductionImpl implements KeyProvider
{
    @Override
    public String getInfuraKey()
    {
        return EthereumNetworkBase.DEFAULT_INFURA_KEY;
    }

    @Override
    public String getBSCExplorerKey()
    {
        return null;
    }

    @Override
    public String getAnalyticsKey()
    {
        return null;
    }

    @Override
    public String getEtherscanKey()
    {
        return null;
    }

    @Override
    public String getPolygonScanKey()
    {
        return null;
    }

    @Override
    public String getAuroraScanKey()
    {
        return null;
    }

    @Override
    public String getCovalentKey()
    {
        return null;
    }

    @Override
    public String getKlaytnKey()
    {
        return null;
    }

    @Override
    public String getSecondaryInfuraKey()
    {
        return null;
    }

    @Override
    public String getTertiaryInfuraKey()
    {
        return null;
    }

    @Override
    public String getRampKey()
    {
        return null;
    }

    @Override
    public String getOpenSeaKey()
    {
        return null;
    }

    @Override
    public String getMailchimpKey()
    {
        return null;
    }

    @Override
    public String getCoinbasePayAppId()
    {
        return null;
    }

    @Override
    public String getWalletConnectProjectId()
    {
        return null;
    }

    @Override
    public String getInfuraSecret()
    {
        return null;
    }

    @Override
    public String getUnstoppableDomainsKey()
    {
        return null;
    }

    @Override
    public String getOkLinkKey()
    {
        return null;
    }

    @Override
    public String getBlockPiBaobabKey()
    {
        return null;
    }

    @Override
    public String getBlockPiCypressKey()
    {
        return null;
    }

    @Override
    public String getBlockNativeKey()
    {
        return null;
    }

    @Override
    public String getSmartPassKey()
    {
        return null;
    }

    @Override
    public String getSmartPassDevKey()
    {
        return null;
    }
}
