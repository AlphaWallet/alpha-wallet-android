package io.stormbird.wallet.entity;

/**
 * Created by James on 19/07/2019.
 * Stormbird in Sydney
 */
public interface PinAuthenticationCallbackInterface
{
    void CompleteAuthentication(int taskCode);
    void FailedAuthentication(int taskCode);
}
