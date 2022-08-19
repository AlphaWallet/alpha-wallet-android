package com.alphawallet.shadows;

import com.alphawallet.app.di.mock.KeyProviderMockImpl;
import com.alphawallet.app.repository.KeyProvider;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.alphawallet.app.repository.KeyProviderJNIImpl;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

@Implements(KeyProviderFactory.class)
public class ShadowKeyProviderFactory
{
    @Implementation
    public static KeyProvider get() {
        return new KeyProviderMockImpl();
    }
}
