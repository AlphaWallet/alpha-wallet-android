package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.wallet.entity.Wallet;

public interface OnBackupClickListener {
    void onBackupClick(View view, Wallet wallet);
}
