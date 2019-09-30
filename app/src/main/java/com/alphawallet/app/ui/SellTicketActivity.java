package com.alphawallet.app.ui;

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

import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.adapter.TicketSaleAdapter;
import com.alphawallet.app.util.BalanceUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.viewmodel.SellTicketModel;
import com.alphawallet.app.viewmodel.SellTicketModelFactory;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import com.alphawallet.token.entity.TicketRange;

import static com.alphawallet.app.C.Key.TICKET;

/**
 * Created by James on 13/02/2018.
 */

public class SellTicketActivity extends BaseActivity implements OnTokenClickListener
{
    @Inject
    protected SellTicketModelFactory viewModelFactory;
    protected SellTicketModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;

    public TextView ids;
    public TextView selected;

    private FinishReceiver finishReceiver;

    private String address;
    private Token token;
    private TicketRange ticketRange;
    private TicketSaleAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_ticket);
        toolbar();
        setTitle(getString(R.string.empty));

        token = getIntent().getParcelableExtra(TICKET);

        address = token.getAddress();

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

        adapter = new TicketSaleAdapter(this, token, viewModel.getAssetDefinitionService());
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
        viewModel.prepare(token);
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

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected) {
        Context context = view.getContext();
        //TODO: what action should be performed when clicking on a range?
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }
}
