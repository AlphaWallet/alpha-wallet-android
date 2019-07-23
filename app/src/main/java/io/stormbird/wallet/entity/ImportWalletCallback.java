package io.stormbird.wallet.entity;

import io.stormbird.wallet.service.HDKeyService;

public interface ImportWalletCallback
{
    void WalletValidated(String address, HDKeyService.AuthenticationLevel level);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
}
