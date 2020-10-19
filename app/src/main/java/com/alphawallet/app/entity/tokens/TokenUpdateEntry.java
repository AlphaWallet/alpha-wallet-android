package com.alphawallet.app.entity.tokens;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.repository.EthereumNetworkRepository;

/**
 * Created by JB on 13/07/2020.
 */
public class TokenUpdateEntry
{
    public final int chainId;
    public final String tokenAddress;
    public ContractType type;
    public long lastUpdateTime;
    public long lastTxCheck;
    public float balanceUpdateWeight;

    public TokenUpdateEntry(int chainId, String tokenAddress, ContractType type)
    {
        this.chainId = chainId;
        this.tokenAddress = tokenAddress;
        this.type = type;
    }

    public boolean isEthereum()
    {
        return type == ContractType.ETHEREUM;
    }

    public boolean needsTransactionCheck()
    {
        switch (type)
        {
            case ERC875_LEGACY:
            case ERC875:
            case ETHEREUM:
            case ERC20:
            case ERC721_TICKET:
                return true;
            case CURRENCY:
            case DELETED_ACCOUNT:
            case OTHER:
            case NOT_SET:
            case ERC721:
            case ERC721_LEGACY:
            case ERC721_UNDETERMINED:
            case CREATION:
            default:
                return false;
        }
    }
}
