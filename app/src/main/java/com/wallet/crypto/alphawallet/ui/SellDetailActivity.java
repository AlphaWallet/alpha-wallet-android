package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketSaleAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.util.BalanceUtils;
import com.wallet.crypto.alphawallet.viewmodel.SellDetailModel;
import com.wallet.crypto.alphawallet.viewmodel.SellDetailModelFactory;
import com.wallet.crypto.alphawallet.viewmodel.SellTicketModel;
import com.wallet.crypto.alphawallet.viewmodel.SellTicketModelFactory;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailActivity extends BaseActivity
{
    @Inject
    protected SellDetailModelFactory viewModelFactory;
    protected SellDetailModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;
    private TextView usdPrice;
    private Button sell;

    private EditText sellPrice;     //TODO: Numeric
    private EditText count;         //TODO: Numeric
    private String ticketIds;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);

        setContentView(R.layout.activity_set_price); //use token just provides a simple list view.

        //we should import a token and a list of chosen ids

        RecyclerView list = findViewById(R.id.listTickets);
        LinearLayout buttons = findViewById(R.id.layoutButtons);
        buttons.setVisibility(View.GONE);

        RelativeLayout rLL = findViewById(R.id.contract_address_layout);
        rLL.setVisibility(View.GONE);

        adapter = new TicketSaleAdapter(this::onTicketIdClick, ticket);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        toolbar();

        setTitle(getString(R.string.action_sell));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SellDetailModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(ticket);
    }

    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT ).show();
    }

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }
}

