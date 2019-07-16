package io.stormbird.wallet.entity;

import android.content.Context;

public interface CreateWalletCallbackInterface
{
    void HDKeyCreated(String address, Context ctx);
    void tryAgain();
    void cancelAuthentication();
    void FetchMnemonic(String mnemonic);
}
