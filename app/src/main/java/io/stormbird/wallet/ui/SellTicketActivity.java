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
import android.widget.Button;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.ui.widget.adapter.TicketSaleAdapter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.viewmodel.SellTicketModel;
import io.stormbird.wallet.viewmodel.SellTicketModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class SellTicketActivity extends BaseActivity {
    @Inject
    protected SellTicketModelFactory viewModelFactory;
    protected SellTicketModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    public TextView ids;
    public TextView selected;

    private FinishReceiver finishReceiver;

    private String address;
    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);

        address = ticket.getAddress();

        toolbar();

        address = ticket.tokenInfo.address;

        setTitle(getString(R.string.empty));

        setContentView(R.layout.activity_sell_ticket);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SellTicketModel.class);

        setupSalesOrder();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.marketQueueSuccessDialog().observe(this, this::displayMarketQueueSuccessDialog);
        viewModel.marketQueueErrorDialog().observe(this, this::displayMarketQueueErrorDialog);

        Button marketPlace = findViewById(R.id.button_marketplace);
        marketPlace.setOnClickListener(v -> {
            onMarketPlace();
        });

        Button universalLink = findViewById(R.id.button_universal_link);
        universalLink.setOnClickListener(v -> {
            onUniversalLink();
        });

        finishReceiver = new FinishReceiver(this);
    }

    private void setupSalesOrder() {
        ticketRange = null;

        RecyclerView list = findViewById(R.id.listTickets);

        adapter = new TicketSaleAdapter(this::onTicketIdClick, ticket, viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.send_menu, menu);
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
//        switch (item.getItemId()) {
//            case R.id.action_next: {
//                onNext();
//            }
//            break;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(ticket);
    }

    private String getIDSelection() {
        List<TicketRange> sellRange = adapter.getCheckedItems();
        List<BigInteger> idList = new ArrayList<>();
        for (TicketRange tr : sellRange)
        {
            idList.addAll(tr.tokenIds);
        }
        Ticket ticket = (Ticket) viewModel.ticket().getValue();
        return ticket.intArrayToString(idList, false);
    }

    private void onMarketPlace() {
        String selection = getIDSelection();

        if (selection != null && selection.length() > 0)
        {
            viewModel.openMarketDialog(this, selection);
        }
    }

    private void onUniversalLink() {
        String selection = getIDSelection();

        if (selection != null && selection.length() > 0) {
            viewModel.openUniversalLinkDialog(this, selection);
        }
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void onSelected(String selectionStr) {
        selected.setText(selectionStr);
    }

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }
}
