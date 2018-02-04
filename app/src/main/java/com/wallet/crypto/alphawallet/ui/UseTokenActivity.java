package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketInfo;
import com.wallet.crypto.alphawallet.viewmodel.UseTokenViewModel;
import com.wallet.crypto.alphawallet.viewmodel.UseTokenViewModelFactory;
import com.wallet.crypto.alphawallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;

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
    private Ticket ticket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_use_token);
        toolbar();

        ticket = getIntent().getParcelableExtra(TICKET);

        TicketInfo info = ticket.ticketInfo;

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        name = findViewById(R.id.textViewName);
        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);
        balance = findViewById(R.id.textViewBalance);

        name.setText(info.name);
        venue.setText(info.venue);
        date.setText(info.date);
        price.setText(String.valueOf(info.price));
        balance.setText(ticket.ticketInfo.populateIDs(ticket.balanceArray, false));
        address = info.address;

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
                viewModel.showRotatingSignature(this, ticket);
            } break;
            case R.id.button_sell: {

            } break;
            case R.id.button_transfer: {
                viewModel.showTransferToken(this, ticket);
            } break;
        }
    }
}
