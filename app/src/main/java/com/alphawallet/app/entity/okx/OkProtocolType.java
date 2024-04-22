package com.alphawallet.app.entity.okx;

import com.alphawallet.app.entity.ContractType;

public enum OkProtocolType
{
    ERC_20("token_20"),
    ERC_721("token_721"),
    ERC_1155("token_1155");

    private final String type;

    OkProtocolType(String type)
    {
        this.type = type;
    }

    public static ContractType getStandardType(OkProtocolType type)
    {
        switch (type)
        {
            case ERC_20 ->
            {
                return ContractType.ERC20;
            }
            case ERC_721 ->
            {
                return ContractType.ERC721;
            }
            case ERC_1155 ->
            {
                return ContractType.ERC1155;
            }
        }

        return ContractType.ERC20;
    }

    public String getValue()
    {
        return type;
    }
}
