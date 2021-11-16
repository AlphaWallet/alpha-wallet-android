package com.alphawallet.app.entity;

import com.alphawallet.token.entity.Signable;

/**
 * Created by James on 21/07/2019.
 * Stormbird in Sydney
 */
public interface SignAuthenticationCallback
{
    void gotAuthorisation(boolean gotAuth);
    default void gotAuthorisationForSigning(boolean gotAuth, Signable messageToSign) { } //if you implement message signing
    default void createdKey(String keyAddress) { }

    void cancelAuthentication();
}