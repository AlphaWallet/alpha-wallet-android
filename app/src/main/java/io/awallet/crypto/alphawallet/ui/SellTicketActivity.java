package io.awallet.crypto.alphawallet.ui;

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

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.ui.widget.adapter.TicketSaleAdapter;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;
import io.awallet.crypto.alphawallet.util.BalanceUtils;
import io.awallet.crypto.alphawallet.viewmodel.SellTicketModel;
import io.awallet.crypto.alphawallet.viewmodel.SellTicketModelFactory;
import io.awallet.crypto.alphawallet.widget.ProgressView;
import io.awallet.crypto.alphawallet.widget.SystemView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static io.awallet.crypto.alphawallet.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class SellTicketActivity extends BaseActivity
{
    @Inject
    protected SellTicketModelFactory viewModelFactory;
    protected SellTicketModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    public TextView ids;
    public TextView selected;

    private String address;
    private Ticket ticket;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        ticket = getIntent().getParcelableExtra(TICKET);
        setupSalesOrder();

        address = ticket.getAddress();

        toolbar();

        address = ticket.tokenInfo.address;

//        setTitle(getString(R.string.market_queue_title));
        setTitle(getString(R.string.empty));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SellTicketModel.class);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.marketQueueSuccessDialog().observe(this, this::displayMarketQueueSuccessDialog);
        viewModel.marketQueueErrorDialog().observe(this, this::displayMarketQueueErrorDialog);

//        Button nextButton = findViewById(R.id.button_next);
//        nextButton.setOnClickListener(v -> {
//            onNext();
//        });

        Button marketPlace = findViewById(R.id.button_marketplace);
        marketPlace.setOnClickListener(v -> {
            onMarketPlace();
        });

        Button magicLink = findViewById(R.id.button_magiclink);
        magicLink.setOnClickListener(v -> {
            onMagicLink();
        });

    }

    private void setupSalesOrder()
    {
        ticketRange = null;
        setContentView(R.layout.activity_sell_ticket);

        RecyclerView list = findViewById(R.id.listTickets);

        adapter = new TicketSaleAdapter(this::onTicketIdClick, ticket);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.send_menu, menu);
        return super.onCreateOptionsMenu(menu);
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

    private String getIDSelection()
    {
        List<TicketRange> sellRange = adapter.getCheckedItems();
        List<Integer> idList = new ArrayList<>();
        for (TicketRange tr : sellRange)
        {
            idList.addAll(tr.tokenIds);
        }

        return viewModel.ticket().getValue().populateIDs(idList, false);
    }

    private void onMarketPlace()
    {
        String selection = getIDSelection();

        if (selection != null && selection.length() > 0) {
            viewModel.openMarketDialog(this, selection);
        }
    }

    private void onMagicLink()
    {
        String selection = getIDSelection();

        if (selection != null && selection.length() > 0) {
            viewModel.openMagicLinkDialog(this, selection);
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

    private void onSelected(String selectionStr)
    {
        selected.setText(selectionStr);
    }

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }
}
