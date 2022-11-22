package com.alphawallet.app.entity;

/**
 * Created by James on 21/07/2019.
 * Stormbird in Sydney
 */
public interface SignAuthenticationCallback
{
    void gotAuthorisation(boolean gotAuth);
    default void createdKey(String keyAddress) { }

    void cancelAuthentication();
}
