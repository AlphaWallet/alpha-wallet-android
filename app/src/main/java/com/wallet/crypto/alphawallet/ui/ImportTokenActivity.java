package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.viewmodel.ImportTokenViewModel;
import com.wallet.crypto.alphawallet.viewmodel.ImportTokenViewModelFactory;
import com.wallet.crypto.alphawallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.IMPORT_STRING;
import static com.wallet.crypto.alphawallet.C.Key.WALLET;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    protected ImportTokenViewModelFactory importTokenViewModelFactory;
    private ImportTokenViewModel viewModel;
    private SystemView systemView;

    private TicketRange ticketRange;
    private String importString;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_import_token);
        toolbar();

        setTitle(getString(R.string.empty));

        importString = getIntent().getParcelableExtra(IMPORT_STRING);
        findViewById(R.id.advanced_options).setVisibility(View.VISIBLE); //setOnClickListener(this);

        Button importTickets = findViewById(R.id.advanced_options);
        importTickets.setOnClickListener(this);

        TextView importTxt = findViewById(R.id.textImport);
        importTxt.setText(importString);

        viewModel = ViewModelProviders.of(this, importTokenViewModelFactory)
                .get(ImportTokenViewModel.class);

        //display the raw import data


    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(importString);
    }

    @Override
    public void onClick(View v) {

    }
}
