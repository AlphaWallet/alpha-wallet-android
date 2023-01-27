package com.alphawallet.app.entity.analytics;

public enum FirstWalletAction
{
    CREATE_WALLET("create"),
    IMPORT_WALLET("import"),
    WATCH_WALLET("watch");

    public static final String KEY = "type";

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
