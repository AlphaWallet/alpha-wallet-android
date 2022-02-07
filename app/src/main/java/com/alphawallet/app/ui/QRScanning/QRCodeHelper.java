package com.alphawallet.app.ui.QRScanning;

public class QRCodeHelper
{
    public static boolean isWalletConnectV1(String text)
    {
        return text.contains("@1?");
    }
}
