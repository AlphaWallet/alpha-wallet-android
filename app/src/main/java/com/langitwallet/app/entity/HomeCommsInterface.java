package com.langitwallet.app.entity;

public interface HomeCommsInterface
{
    void requestNotificationPermission();
    void backupSuccess(String keyAddress);
    void resetTokens();
    void resetTransactions();
}
