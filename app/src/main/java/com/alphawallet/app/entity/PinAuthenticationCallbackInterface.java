package com.alphawallet.app.entity;

/**
 * Created by James on 19/07/2019.
 * Stormbird in Sydney
 */
public interface PinAuthenticationCallbackInterface
{
    void completeAuthentication(Operation taskCode);
    void failedAuthentication(Operation taskCode);
}
