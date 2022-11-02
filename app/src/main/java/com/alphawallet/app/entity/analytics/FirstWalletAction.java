package com.alphawallet.app.entity.analytics;

public enum FirstWalletAction
{
    CREATE_WALLET("Create Wallet"),
    IMPORT_WALLET("Import Wallet");

    public static final String KEY = "action";

    private final String action;

    FirstWalletAction(String action)
    {
        this.action = action;
    }

    public String getValue()
    {
        return action;
    }
}
