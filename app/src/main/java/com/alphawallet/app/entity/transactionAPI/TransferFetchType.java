package com.alphawallet.app.entity.transactionAPI;

/**
 * Created by JB on 10/02/2023.
 */
public enum TransferFetchType
{
    tokentx("tokentx"),
    tokennfttx("tokennfttx"),
    token1155tx("token1155tx");

    private final String type;

    TransferFetchType(String type)
    {
        this.type = type;
    }

    public String getValue()
    {
        return type;
    }
}
