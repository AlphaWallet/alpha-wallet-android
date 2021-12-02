package com.alphawallet.app.entity;

/**
 * Created by James on 22/07/2019.
 * Stormbird in Sydney
 */
public enum WalletType
{
    NOT_DEFINED,
    KEYSTORE,
    HDKEY,
    WATCH,
    TEXT_MARKER, // used as a separator in wallet view
    KEYSTORE_LEGACY,  // to support keys created from old wallets
    LARGE_TITLE
}
