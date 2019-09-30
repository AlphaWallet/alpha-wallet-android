package com.alphawallet.app.entity;

//Used mainly for re-entrant encrypt/decrypt operations for types of keys
public enum Operation
{
    CREATE_HD_KEY, FETCH_MNEMONIC, IMPORT_HD_KEY, CHECK_AUTHENTICATION, SIGN_DATA,
    UPGRADE_HD_KEY, CREATE_KEYSTORE_KEY, UPGRADE_KEYSTORE_KEY, CREATE_PRIVATE_KEY
}
