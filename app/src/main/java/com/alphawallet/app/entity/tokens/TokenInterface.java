package com.alphawallet.app.entity.tokens;

public interface TokenInterface
{
    void resetTokens();
    void changedLocale();
    void addedToken(int[] chainIds, String[] addrs);
    default void refreshTokens() { };
}
