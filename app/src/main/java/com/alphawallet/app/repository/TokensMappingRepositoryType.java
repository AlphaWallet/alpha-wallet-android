package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.token.entity.ContractAddress;

public interface TokensMappingRepositoryType
{
    TokenGroup getTokenGroup(long chainId, String address, ContractType type);

    ContractAddress getBaseToken(long chainId, String address);
}
