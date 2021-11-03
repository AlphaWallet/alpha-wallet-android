package com.alphawallet.app.ui.widget.entity;

import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

public class ManageTokensData {
    public final String walletAddress;
    public final ActivityResultLauncher<Intent> launcher;

    public ManageTokensData(String walletAddress, ActivityResultLauncher<Intent> launcher) {
        this.walletAddress = walletAddress;
        this.launcher = launcher;
    }
}
