package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.router.SendTokenRouter;
import com.wallet.crypto.trustapp.viewmodel.UseTokenViewModel;
import com.wallet.crypto.trustapp.viewmodel.UseTokenViewModelFactory;
import com.wallet.crypto.trustapp.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Created by James on 22/01/2018.
 */

public class UseTokenActivity extends BaseActivity implements View.OnClickListener
{
    @Inject
    protected UseTokenViewModelFactory useTokenViewModelFactory;
    private UseTokenViewModel viewModel;
    private SystemView systemView;

    public TextView name;
    public TextView venue;
    public TextView date;
    public TextView price;
    public TextView balance;

    private String address;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_use_token);
        toolbar();

        String tName = getIntent().getStringExtra(C.EXTRA_CONTRACT_NAME);
        String tVenue = getIntent().getStringExtra(C.EXTRA_TICKET_VENUE);
        String tDate = getIntent().getStringExtra(C.EXTRA_TICKET_DATE);
        double tPrice = getIntent().getDoubleExtra(C.EXTRA_TICKET_PRICE, 0);
        double dbBalance = getIntent().getDoubleExtra(C.EXTRA_TOKEN_BALANCE ,0);
        address = getIntent().getStringExtra(C.EXTRA_ADDRESS);

        int tBalance = (int)dbBalance;

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        name = findViewById(R.id.textViewName);
        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);
        balance = findViewById(R.id.textViewBalance);

        name.setText(tName);
        venue.setText(tVenue);
        date.setText(tDate);
        price.setText(String.valueOf(tPrice));
        balance.setText(String.valueOf(tBalance));

        viewModel = ViewModelProviders.of(this, useTokenViewModelFactory)
                .get(UseTokenViewModel.class);

        findViewById(R.id.button_use).setOnClickListener(this);
        findViewById(R.id.button_sell).setOnClickListener(this);
        findViewById(R.id.button_transfer).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.button_use: {
                viewModel.showRotatingSignature(this);
            } break;
            case R.id.button_sell: {

            } break;
            case R.id.button_transfer: {
                viewModel.showTransferToken(this, address);
            } break;
        }
    }
}
