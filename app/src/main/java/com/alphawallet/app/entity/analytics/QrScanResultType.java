package com.alphawallet.app.entity.analytics;

public enum QrScanResultType
{
    ADDRESS_OR_EIP_681("addressOrEip681"),
    WALLET_CONNECT("walletConnect"),
    STRING("string"),
    URL("url"),
    PRIVATE_KEY("privateKey"),
    SEED_PHRASE("seedPhrase"),
    JSON("json"),
    ADDRESS("address");

    public static final String KEY = "resultType";

    private final String resultType;

    QrScanResultType(String resultType)
    {
        this.resultType = resultType;
    }

    public String getValue()
    {
        return resultType;
    }
}
