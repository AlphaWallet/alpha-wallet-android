package com.alphawallet.app.entity;

public interface HomeCommsInterface
{
    void downloadReady(String ready);
    void requestNotificationPermission();
    void backupSuccess(String keyAddress);
    void resetTokens();
    void resetTransactions();
    void openWalletConnect(String sessionId);
}
