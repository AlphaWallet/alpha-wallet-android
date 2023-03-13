package com.alphawallet.app.entity;

import com.alphawallet.hardware.SignatureFromKey;

/**
 * Created by James on 21/07/2019.
 * Stormbird in Sydney
 */
public interface SignAuthenticationCallback
{
    void gotAuthorisation(boolean gotAuth);

    default void createdKey(String keyAddress)
    {
    }

    void cancelAuthentication();

    void gotSignature(SignatureFromKey signature);

    default void signingError(String error)
    {
    } //Handle signing error from hardware card
}
