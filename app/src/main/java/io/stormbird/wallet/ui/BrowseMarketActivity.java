package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.MagicLinkParcel;
import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.entity.OrderContractAddressPair;
import io.stormbird.wallet.ui.widget.adapter.ERC875MarketAdapter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.viewmodel.BrowseMarketViewModel;
import io.stormbird.wallet.viewmodel.BrowseMarketViewModelFactory;
import io.stormbird.wallet.widget.FilterDialog;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SearchDialog;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.MagicLinkData;

import static io.stormbird.wallet.C.Key.MARKETPLACE_EVENT;

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

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(BrowseMarketViewModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.updateMarket().observe(this, this::onMarketUpdate); //receive the sales orders (some may be invalid - TODO: display progress indeterminate)
        viewModel.updateBalance().observe(this, this::updateBalance);
        viewModel.startUpdate().observe(this, this::startUpdate);
        viewModel.endUpdate().observe(this, this::endUpdate);

        setupSearchBar();
    }

    private void updateBalance(OrderContractAddressPair balanceUpdate)
    {
        //now update the balances of the tokens and corresponding posts
        adapter.updateContent(balanceUpdate);
    }

    private void setupSearchBar() {
        SearchDialog dialog = new SearchDialog(this);
        RelativeLayout searchLayout = findViewById(R.id.search_container);
        searchLayout.setOnClickListener(v -> {
            dialog.show();
        });
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

    private void onMarketUpdate(MagicLinkData[] trades)
    {
        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new ERC875MarketAdapter(this::onOrderClick, trades);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        systemView.hide();
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

    private void onOrderClick(View view, MagicLinkData instance) {
        MagicLinkParcel parcel = new MagicLinkParcel(instance);
        //TODO: just clicked on an order.
        viewModel.showPurchaseTicket(this, parcel);
    }

    private void startUpdate(Boolean dummy) {
        adapter.startUpdate();
    }
    private void endUpdate(Boolean dummy) {
        adapter.endUpdate();
    }
}
