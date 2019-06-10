package io.stormbird.wallet.entity;

/**
 * Created by James on 9/06/2019.
 * Stormbird in Sydney
 */
public interface AuthenticationCallback
{
    void authenticatePass(int callbackId);
    void authenticateFail(String fail);
}
