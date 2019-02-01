package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.adapter.TicketSaleAdapter;
import io.stormbird.wallet.viewmodel.RedeemAssetSelectViewModel;
import io.stormbird.wallet.viewmodel.RedeemAssetSelectViewModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TicketRange;

import java.math.BigInteger;

import static io.stormbird.wallet.C.Key.TICKET;

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

    private FinishReceiver finishReceiver;

    public TextView ids;
    public TextView selected;

    private Button nextButton;
    private Button redeemButton;

    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);
        setContentView(R.layout.activity_redeem_asset);

        nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> {
            onNext();
        });

        redeemButton = findViewById(R.id.button_redeem);
        redeemButton.setOnClickListener(v -> {
            onRedeem();
        });

        toolbar();

//        setTitle(getString(R.string.title_redeem_token));

        setTitle(getString(R.string.empty));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(RedeemAssetSelectViewModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        finishReceiver = new FinishReceiver(this);
        setupRedeemSelector();
    }

    private void setupRedeemSelector()
    {
        ticketRange = null;

        currentMenu = R.menu.send_menu;
        invalidateOptionsMenu();

        RecyclerView list = findViewById(R.id.listTickets);

        adapter = new TicketSaleAdapter(this::onTokenClick, ticket, viewModel.getAssetDefinitionService());
        adapter.setRedeemTicket(ticket);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        nextButton.setVisibility(View.VISIBLE);
        redeemButton.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(currentMenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
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
            case android.R.id.home: {
                if (currentMenu == R.menu.redeem_menu) {
                    setupRedeemSelector();
                    return true;
                }
            }
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
            onTokenClick(null, ticket, range.tokenIds.get(0));

            adapter.setRedeemTicketQuantity(range, ticket);
            RecyclerView list = findViewById(R.id.listTickets);
            list.setAdapter(null);
            list.setAdapter(adapter);

            nextButton.setVisibility(View.GONE);
            redeemButton.setVisibility(View.VISIBLE);
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

    private void onTokenClick(View v, Token token, BigInteger id) {
        currentMenu = R.menu.redeem_menu;
        invalidateOptionsMenu();
//        adapter.setRedeemTicketQuantity(range, ticket);
//        RecyclerView list = findViewById(R.id.listTickets);
//        list.setAdapter(null);
//        list.setAdapter(adapter);
    }
}
