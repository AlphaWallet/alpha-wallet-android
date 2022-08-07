package com.alphawallet.app.di.mock;

import com.alphawallet.app.repository.KeyProvider;

public class KeyProviderMockImpl implements KeyProvider
{
    @Override
    public String getBSCExplorerKey()
    {
        return "mock-bsc-explorer-key";
    }
}
