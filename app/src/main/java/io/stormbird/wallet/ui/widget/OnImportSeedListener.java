package io.stormbird.wallet.ui.widget;

import android.app.Activity;

public interface OnImportSeedListener
{
    void onSeed(String seedPhrase, Activity ctx);
}
