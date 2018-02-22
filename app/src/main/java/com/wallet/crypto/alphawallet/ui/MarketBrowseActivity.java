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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.MarketInstance;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.ui.widget.adapter.ERC875MarketAdapter;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TicketSaleAdapter;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import com.wallet.crypto.alphawallet.util.BalanceUtils;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;
import com.wallet.crypto.alphawallet.viewmodel.MarketBrowseModel;
import com.wallet.crypto.alphawallet.viewmodel.MarketBrowseModelFactory;
import com.wallet.crypto.alphawallet.viewmodel.SellTicketModel;
import com.wallet.crypto.alphawallet.viewmodel.SellTicketModelFactory;
import com.wallet.crypto.alphawallet.widget.ProgressView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Created by James on 19/02/2018.
 */

public class MarketBrowseActivity extends BaseActivity
{
    @Inject
    protected MarketBrowseModelFactory viewModelFactory;
    protected MarketBrowseModel viewModel;
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

        setupMarketOrder();
        toolbar();

        setTitle(getString(R.string.market_buy_title));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(MarketBrowseModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.updateMarket().observe(this, this::onMarketUpdate);
    }

    private void setupMarketOrder()
    {
        setContentView(R.layout.activity_use_token); //use token just provides a simple list view.

        LinearLayout buttons = findViewById(R.id.layoutButtons);
        buttons.setVisibility(View.GONE);

        RelativeLayout rLL = findViewById(R.id.contract_address_layout);
        rLL.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void onMarketUpdate(MarketInstance[] trades)
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

    private void onOrderClick(View view, MarketInstance instance) {
        Context context = view.getContext();
        //TODO: just clicked on an order.
    }
}
