package io.stormbird.wallet.entity;

import io.stormbird.wallet.service.HDKeyService;

public interface ImportWalletCallback
{
    void WalletValidated(String address, HDKeyService.AuthenticationLevel level);
    void KeystoreValidated(String address, String newPassword, String keystoreDetails, HDKeyService.AuthenticationLevel level);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
    void KeyValidated(String privateKey, String newPassword, HDKeyService.AuthenticationLevel authLevel);
}
