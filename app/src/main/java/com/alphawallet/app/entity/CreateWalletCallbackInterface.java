package com.alphawallet.app.entity;

import com.alphawallet.app.service.KeyService;

public interface CreateWalletCallbackInterface
{
    void HDKeyCreated(String address, KeyService.AuthenticationLevel level);
    void keyFailure(String message);
    void cancelAuthentication();
    void FetchMnemonic(String mnemonic);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
}
