package com.alphawallet.app.di.mock;

import com.alphawallet.app.repository.KeyProvider;

public class KeyProviderMockImpl implements KeyProvider
{
    private static final String FAKE_KEY_FOR_TESTING = "fake-key-for-testing";

    @Override
    public String getBSCExplorerKey()
    {
        return "mock-bsc-explorer-key";
    }

    @Override
    public String getAnalyticsKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getEtherscanKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getPolygonScanKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getAuroraScanKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getCovalentKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getKlaytnKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getInfuraKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getSecondaryInfuraKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getRampKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getOpenSeaKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getMailchimpKey()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getCoinbasePayAppId()
    {
        return FAKE_KEY_FOR_TESTING;
    }

    @Override
    public String getWalletConnectProjectId()
    {
        return FAKE_KEY_FOR_TESTING;
    }
}
