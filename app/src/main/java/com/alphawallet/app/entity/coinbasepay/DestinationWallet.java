package com.alphawallet.app.entity.coinbasepay;

import java.util.List;

public class DestinationWallet
{
    final transient Type type;
    String address;
    List<String> blockchains;
    List<String> assets;

    public DestinationWallet(Type type, String address, List<String> list)
    {
        this.type = type;
        this.address = address;
        if (type.equals(Type.ASSETS))
        {
            this.assets = list;
        }
        else
        {
            this.blockchains = list;
        }
    }

    public enum Type
    {
        ASSETS,
        BLOCKCHAINS
    }
}
