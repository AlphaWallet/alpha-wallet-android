package com.alphawallet.app.entity.analytics;

public enum QrScanSource
{
    ADDRESS_TEXT_FIELD("addressTextField"),
    BROWSER_SCREEN("browserScreen"),
    IMPORT_WALLET_SCREEN("importWalletScreen"),
    ADD_CUSTOM_TOKEN_SCREEN("addCustomTokenScreen"),
    WALLET_SCREEN("walletScreen"),
    SEND_FUNGIBLE_SCREEN("sendFungibleScreen"),
    QUICK_ACTION("quickAction");

    public static final String KEY = "source";

    private final String source;

    QrScanSource(String source)
    {
        this.source = source;
    }

    public String getValue()
    {
        return source;
    }
}
