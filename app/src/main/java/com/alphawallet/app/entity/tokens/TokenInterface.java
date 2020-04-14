package com.alphawallet.app.entity.tokens;

import com.alphawallet.app.entity.ContractLocator;

import java.util.List;

public interface TokenInterface
{
    void resetTokens();
    void changedLocale();
    void addedToken(List<ContractLocator> tokenContracts);
    default void refreshTokens() { };
}
