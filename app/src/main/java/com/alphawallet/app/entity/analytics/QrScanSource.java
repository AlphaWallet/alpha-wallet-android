package com.alphawallet.app.entity.analytics;

public enum QrScanSource
{
    WALLET_CONNECT("Wallet Connect"),
    ADDRESS_TEXT_FIELD("Address Text Field"),
    BROWSER_SCREEN("Browser Screen"),
    IMPORT_WALLET_SCREEN("Import Wallet Screen"),
    ADD_CUSTOM_TOKEN_SCREEN("Add Custom Token Screen"),
    WALLET_SCREEN("Wallet Screen"),
    SEND_FUNGIBLE_SCREEN("Send Screen"),
    QUICK_ACTION("Quick Action");

    public static final String KEY = "qr_scan_source";

    private final String type;

    QrScanSource(String type)
    {
        this.type = type;
    }

    public String getValue()
    {
        return type;
    }
}
