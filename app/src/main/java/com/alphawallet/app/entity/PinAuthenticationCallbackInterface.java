package com.alphawallet.app.entity;

/**
 * Created by James on 19/07/2019.
 * Stormbird in Sydney
 */
public interface PinAuthenticationCallbackInterface
{
    void CompleteAuthentication(Operation taskCode);
    void FailedAuthentication(Operation taskCode);
}
