package com.alphawallet.shadows;


import com.alphawallet.app.di.mock.KeyProviderMockNonProductionImpl;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(KeyProviderFactory.class)
public class ShadowKeyProviderFactoryNonProduction
{
    @Implementation
    public static KeyProvider get() {
        return new KeyProviderMockNonProductionImpl();
    }
}
