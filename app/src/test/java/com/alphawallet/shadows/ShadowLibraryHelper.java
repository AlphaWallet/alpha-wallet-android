package com.alphawallet.shadows;

import com.alphawallet.app.util.LibraryHelper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(LibraryHelper.class)
public class ShadowLibraryHelper
{
    @Implementation
    public static void loadKeysLibrary() {
        System.loadLibrary("keys");
    }

    @Implementation
    public static void loadKeysTrustWalletCoreLibrary()
    {}
}
