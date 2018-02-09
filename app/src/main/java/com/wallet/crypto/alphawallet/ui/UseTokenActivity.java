package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketInfo;
import com.wallet.crypto.alphawallet.viewmodel.UseTokenViewModel;
import com.wallet.crypto.alphawallet.viewmodel.UseTokenViewModelFactory;
import com.wallet.crypto.alphawallet.widget.ProgressView;
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
    private ProgressView progressView;

    public TextView name;
    public TextView venue;
    public TextView date;
    public TextView price;
    public TextView balance;

    private Ticket ticket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_use_token);
        toolbar();

        ticket = getIntent().getParcelableExtra(TICKET);

        setTitle(getString(R.string.title_use_token));
        TicketInfo info = ticket.ticketInfo;

        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();

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

        viewModel = ViewModelProviders.of(this, useTokenViewModelFactory)
                .get(UseTokenViewModel.class);

        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);

        findViewById(R.id.button_use).setOnClickListener(this);
        findViewById(R.id.button_sell).setOnClickListener(this);
        findViewById(R.id.button_transfer).setOnClickListener(this);
        findViewById(R.id.copy_address).setOnClickListener(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_use:
            {
                viewModel.showRotatingSignature(this, ticket);
            }
            break;
            case R.id.button_sell:
            {
                viewModel.showMarketOrder(this, ticket);
            }
            break;
            case R.id.button_transfer:
            {
                viewModel.showTransferToken(this, ticket);
            }
            break;
            case R.id.copy_address:
            {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getResources().getString(R.string.copy_addr_to_clipboard), ticket.getAddress());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.copy_addr_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT ).show();
    }
}
