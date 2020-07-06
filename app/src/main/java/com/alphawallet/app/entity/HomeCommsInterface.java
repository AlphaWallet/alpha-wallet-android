package com.alphawallet.app.entity;

import java.util.List;

public interface HomeCommsInterface
{
    void downloadReady(String ready);
    void resetToolbar();
    void requestNotificationPermission();
    void backupSuccess(String keyAddress);
    void changeCurrency();
    void resetTokens();
    void addedToken(List<ContractLocator> tokenContracts);
    void changedLocale();
    default void refreshTokens() { };
}
