package com.alphawallet.app.entity.analytics;

public enum TokenSwapEvent
{
    NATIVE_SWAP("Native Swap"),
    QUICKSWAP("Quick Swap"),
    ONEINCH("Oneinch"),
    HONEYSWAP("Honeyswap"),
    UNISWAP("Uniswap");

    public static final String KEY = "name";

    private final String value;

    TokenSwapEvent(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}
