package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Address;
import com.wallet.crypto.trustapp.ui.barcode.BarcodeCaptureActivity;
import com.wallet.crypto.trustapp.viewmodel.AddTokenViewModel;
import com.wallet.crypto.trustapp.widget.SystemView;

import dagger.android.AndroidInjection;

/**
 * Created by James on 22/01/2018.
 */

public class UseTokenActivity extends BaseActivity implements View.OnClickListener
{
    private SystemView systemView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_use_token);

        toolbar();

        systemView = findViewById(R.id.system_view);
        systemView.hide();


        findViewById(R.id.save).setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {

    }
}
