package com.alphawallet.app.entity;

/**
 * Created by James on 21/07/2019.
 * Stormbird in Sydney
 */
public interface SignAuthenticationCallback
{
    void GotAuthorisation(boolean gotAuth);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
    default void CreatedKey(String keyAddress) { };
}
