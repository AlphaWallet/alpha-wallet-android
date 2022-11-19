package com.alphawallet.app.repository;

public class KeyProviderJNIImpl implements KeyProvider
{
    public KeyProviderJNIImpl()
    {
        System.loadLibrary("keys");
    }

    public native String getInfuraKey();

    public native String getSecondaryInfuraKey();

    public native String getBSCExplorerKey();

    public native String getAnalyticsKey();

    public native String getEtherscanKey();

    public native String getPolygonScanKey();

    public native String getAuroraScanKey();

    public native String getCovalentKey();

    public native String getKlaytnKey();

    public native String getRampKey();

    public native String getOpenSeaKey();

    public native String getMailchimpKey();

    public native String getCoinbasePayAppId();

    public native String getWalletConnectProjectId();
}
