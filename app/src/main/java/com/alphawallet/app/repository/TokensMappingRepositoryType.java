package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokendata.TokenGroup;

public interface TokensMappingRepositoryType
{
    TokenGroup getTokenGroup(long chainId, String address, ContractType type);
}
