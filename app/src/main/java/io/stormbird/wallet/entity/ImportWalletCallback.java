package io.stormbird.wallet.entity;

public interface ImportWalletCallback
{
    void WalletValidated(String address);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
}
