package com.alphawallet.app.entity;

import java.util.Map;

/**
 * Created by James on 8/11/2018.
 * Stormbird in Singapore
 */

public class WalletUpdate
{
    public long lastBlock;
    public Map<String, Wallet> wallets;

    public WalletUpdate()
    {
        lastBlock = -1L;
    }
}
