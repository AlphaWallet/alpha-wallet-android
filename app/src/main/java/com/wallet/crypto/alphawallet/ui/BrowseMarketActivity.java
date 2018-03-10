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
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.MarketplaceEvent;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.ui.widget.adapter.ERC875MarketAdapter;
import com.wallet.crypto.alphawallet.util.BalanceUtils;
import com.wallet.crypto.alphawallet.viewmodel.BrowseMarketViewModel;
import com.wallet.crypto.alphawallet.viewmodel.BrowseMarketViewModelFactory;
import com.wallet.crypto.alphawallet.widget.FilterDialog;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.alphawallet.C.Key.MARKETPLACE_EVENT;

/**
 * Created by James on 19/02/2018.
 */

public class BrowseMarketActivity extends BaseActivity
{
    @Inject
    protected BrowseMarketViewModelFactory viewModelFactory;
    protected BrowseMarketViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    public TextView name;
    public TextView ids;
    public TextView selected;

    private ERC875MarketAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setupSalesOrder();
        toolbar();

        MarketplaceEvent marketplaceEvent = getIntent().getParcelableExtra(MARKETPLACE_EVENT);

        setTitle(marketplaceEvent.getEventName());

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(BrowseMarketViewModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.updateMarket().observe(this, this::onMarketUpdate);
    }

    private void setupSalesOrder()
    {
        setContentView(R.layout.activity_browse_market); //use token just provides a simple list view.

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_filter, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter: {
                FilterDialog dialog = new FilterDialog(this);
                dialog.show();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMarketUpdate(SalesOrder[] trades)
    {
        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new ERC875MarketAdapter(this::onOrderClick, trades);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void onSelected(String selectionStr)
    {
        selected.setText(selectionStr);
    }

    private void onOrderClick(View view, SalesOrder instance) {
        Context context = view.getContext();
        //TODO: just clicked on an order.
        viewModel.showPurchaseTicket(context, instance);
    }
}
