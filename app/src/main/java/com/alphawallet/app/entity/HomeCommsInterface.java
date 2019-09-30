package com.alphawallet.app.entity;

public interface HomeCommsInterface
{
    void downloadReady(String ready);
    void resetToolbar();
    void requestNotificationPermission();
    void backupSuccess(String keyAddress);
}
