package io.stormbird.wallet.entity;

import android.content.Context;
import io.stormbird.wallet.service.KeyService;

public interface CreateWalletCallbackInterface
{
    void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level);
    void keyFailure(String message);
    void cancelAuthentication();
    void FetchMnemonic(String mnemonic);
    void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback);
}
