package com.alphawallet.app.util;

public class LibraryHelper
{
    public static void loadKeysLibrary()
    {
        System.loadLibrary("keys");
    }

    public static void loadKeysTrustWalletCoreLibrary()
    {
        System.loadLibrary("TrustWalletCore");
    }
}
