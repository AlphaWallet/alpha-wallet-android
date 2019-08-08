package io.stormbird.wallet.entity;

import io.stormbird.wallet.service.KeyService;

public interface ImportWalletCallback
{
    void WalletValidated(String address, KeyService.AuthenticationLevel level);
    void KeystoreValidated(String newPassword, KeyService.AuthenticationLevel level);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
    void KeyValidated(String newPassword, KeyService.AuthenticationLevel authLevel);
}
