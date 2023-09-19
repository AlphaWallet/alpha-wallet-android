package com.alphawallet.app.entity.transactionAPI;

/**
 * Created by JB on 10/02/2023.
 */
public enum TransferFetchType
{
    ETHEREUM("eth"), // dummy type for storing token reads
    ERC_20("tokentx"),
    ERC_721("tokennfttx"),
    ERC_1155("token1155tx");

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
