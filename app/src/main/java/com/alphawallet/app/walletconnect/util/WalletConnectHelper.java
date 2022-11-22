package com.alphawallet.app.walletconnect.util;

public abstract class WalletConnectHelper
{
    public static boolean isWalletConnectV1(String text)
    {
        return text.contains("@1?");
    }

    public static long getChainId(String chainId)
    {
        return Long.parseLong(chainId.split(":")[1]);
    }
}
