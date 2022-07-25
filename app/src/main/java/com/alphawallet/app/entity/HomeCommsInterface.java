package com.alphawallet.app.entity;

public interface HomeCommsInterface
{
    void requestNotificationPermission();
    void backupSuccess(String keyAddress);
    void resetTokens();
    void resetTransactions();
    void openWalletConnect(String sessionId);
}
