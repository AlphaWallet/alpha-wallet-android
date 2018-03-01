package com.wallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketSaleAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.viewmodel.RedeemAssetSelectViewModel;
import com.wallet.crypto.alphawallet.viewmodel.RedeemAssetSelectViewModelFactory;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.TICKET;

/**
 * Created by James on 27/02/2018.
 */

/**
 * This is where we select tickets to redeem
 */
public class RedeemAssetSelectActivity extends BaseActivity
{
    @Inject
    protected RedeemAssetSelectViewModelFactory viewModelFactory;
    protected RedeemAssetSelectViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private int currentMenu = R.menu.send_menu;

    public TextView name;
    public TextView ids;
    public TextView selected;

    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);
        setupRedeemSelector();

        toolbar();

        setTitle(getString(R.string.title_redeem_token));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(RedeemAssetSelectViewModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
    }

    private void setupRedeemSelector()
    {
        ticketRange = null;
        setContentView(R.layout.activity_use_token); //use token just provides a simple list view.

        RecyclerView list = findViewById(R.id.listTickets);
        LinearLayout buttons = findViewById(R.id.layoutButtons);
        buttons.setVisibility(View.GONE);

        RelativeLayout rLL = findViewById(R.id.contract_address_layout);
        rLL.setVisibility(View.GONE);

        adapter = new TicketSaleAdapter(this::onTicketIdClick, ticket);
        adapter.setRedeemTicket(ticket);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(currentMenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next: {
                onNext();
            }
            break;
            case R.id.action_redeem: {
                onRedeem();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    private void onNext() {
        //first get range selection
        TicketRange range = adapter.getCheckedItem();
        if (range != null)
        {
            onTicketIdClick(null, range);
        }
    }

    private void onRedeem()
    {
        int quantity =  adapter.getSelectedQuantity();
        TicketRange range = adapter.getCheckedItem();

        //check params
        if (range != null && quantity > 0 && quantity <= range.tokenIds.size())
        {
            //form a new Ticket Range with the required tickets to burn
            range.selectSubRange(quantity);
            viewModel.showRedeemSignature(this, range, ticket);
        }
    }

    private void onSelected(String selectionStr)
    {
        selected.setText(selectionStr);
    }

    private void onTicketIdClick(View v, TicketRange range) {
        currentMenu = R.menu.redeem_menu;
        invalidateOptionsMenu();
        adapter.setRedeemTicketQuantity(range, ticket);
        RecyclerView list = findViewById(R.id.listTickets);
        list.setAdapter(null);
        list.setAdapter(adapter);
    }
}
