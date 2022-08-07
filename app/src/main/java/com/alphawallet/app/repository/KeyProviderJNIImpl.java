package com.alphawallet.app.repository;

public class KeyProviderJNIImpl implements KeyProvider
{
    public KeyProviderJNIImpl()
    {
        System.loadLibrary("keys");
    }

    public native String getBSCExplorerKey();
}
