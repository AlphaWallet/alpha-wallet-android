package com.langitwallet.app.repository;

import com.alphawallet.token.entity.ContractAddress;
import com.langitwallet.app.entity.ContractType;
import com.langitwallet.app.entity.tokendata.TokenGroup;

public interface TokensMappingRepositoryType
{
    TokenGroup getTokenGroup(long chainId, String address, ContractType type);

    ContractAddress getBaseToken(long chainId, String address);
}
