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
    public long lastUpdateTime;
    public long lastTxCheck;
    public float balanceUpdateWeight;
    public boolean isEthereum;

    public TokenUpdateEntry(int chainId, String tokenAddress, ContractType type)
    {
        this.chainId = chainId;
        this.tokenAddress = tokenAddress;
        this.isEthereum = type == ContractType.ETHEREUM;
    }

    public boolean isEthereum()
    {
        return isEthereum;
    }

    public boolean needsTransactionCheck(ContractType type)
    {
        switch (type)
        {
            case ERC875_LEGACY:
            case ERC875:
            case ETHEREUM:
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
            case ERC20:
            default:
                return false;
        }
    }
}
