package com.alphawallet.app.entity;

/**
 * Created by James on 26/02/2019.
 * Stormbird in Singapore
 */

import com.alphawallet.app.entity.tokens.TokenInfo;

import java.math.BigDecimal;

/**
 * This is a superclass for all contracts.
 * A pure 'contract' class is never created hence declaring as an abstract.
 * Examples:
 * - ERC20 Token
 * - ERC875 Token
 * - ERC721 Token
 */

public abstract class Contract
{
    public TokenInfo tokenInfo;
    public BigDecimal balance;
    public long updateBlancaTime;
    public boolean balanceIsLive = false;
    private String tokenWallet;
    private short tokenNetwork;
    private final boolean requiresAuxRefresh = true;
    protected ContractType contractType;
    public long lastBlockCheck = 0;
}
