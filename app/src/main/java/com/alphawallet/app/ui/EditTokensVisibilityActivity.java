package com.alphawallet.app.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;

import dagger.android.AndroidInjection;

public class EditTokensVisibilityActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
    }
}
