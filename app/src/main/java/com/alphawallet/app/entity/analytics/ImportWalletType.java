package com.alphawallet.app.entity.analytics;

public enum ImportWalletType
{
    SEED_PHRASE("Seed Phrase"),
    KEYSTORE("Keystore"),
    PRIVATE_KEY("Private Key"),
    WATCH("Watch");

    private final String type;

    ImportWalletType(String type)
    {
        this.type = type;
    }

    public String getValue()
    {
        return type;
    }
}
