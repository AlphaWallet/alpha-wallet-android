package com.alphawallet.app.entity;

/**
 * Created by James on 6/12/2018.
 * Stormbird in Singapore
 */
public enum ContractType
{
    NOT_SET,
    ETHEREUM,
    ERC20,
    ERC721,
    ERC875LEGACY,
    ERC875,
    OTHER,
    CURRENCY,
    DELETED_ACCOUNT,
    ERC721_LEGACY,
    CREATION //Placeholder for generic, should be at end of list
}
