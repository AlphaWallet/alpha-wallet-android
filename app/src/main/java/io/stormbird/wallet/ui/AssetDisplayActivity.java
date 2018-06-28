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

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.ui.widget.adapter.TicketAdapter;
import io.stormbird.wallet.viewmodel.AssetDisplayViewModel;
import io.stormbird.wallet.viewmodel.AssetDisplayViewModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 22/01/2018.
 */

/**
 *
 */
public class AssetDisplayActivity extends BaseActivity implements View.OnClickListener
{
    @Inject
    protected AssetDisplayViewModelFactory assetDisplayViewModelFactory;
    private AssetDisplayViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private RecyclerView list;

    private FinishReceiver finishReceiver;

    private Ticket ticket;
    private TicketAdapter adapter;
    private String balance = null;
    private String burnList = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        ticket = getIntent().getParcelableExtra(TICKET);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_display);
        toolbar();

        setTitle(getString(R.string.title_show_tickets));
        TokenInfo info = ticket.tokenInfo;

        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        list = findViewById(R.id.listTickets);

        viewModel = ViewModelProviders.of(this, assetDisplayViewModelFactory)
                .get(AssetDisplayViewModel.class);

        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ticket().observe(this, this::onTokenUpdate);

        adapter = new TicketAdapter(this::onTicketIdClick, ticket, viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.setHapticFeedbackEnabled(true);

        findViewById(R.id.button_use).setOnClickListener(this);
        findViewById(R.id.button_sell).setOnClickListener(this);
        findViewById(R.id.button_transfer).setOnClickListener(this);

        finishReceiver = new FinishReceiver(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(ticket);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }

    private void onTokenUpdate(Token t)
    {
        ticket = (Ticket)t;
        if (!ticket.getBurnListStr().equals(burnList) || !ticket.getFullBalance().equals(balance))
        {
            adapter.setTicket(ticket);
            RecyclerView list = findViewById(R.id.listTickets);
            list.setAdapter(null);
            list.setAdapter(adapter);
            balance = ticket.getFullBalance();
            burnList = ticket.getBurnListStr();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_qr) {
            viewModel.showContractInfo(this, ticket.getAddress());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button_use:
            {
                viewModel.selectAssetIdsToRedeem(this, ticket);
            }
            break;
            case R.id.button_sell:
            {
                viewModel.sellTicketRouter(this, ticket);// showSalesOrder(this, ticket);
            }
            break;
            case R.id.button_transfer:
            {
                viewModel.showTransferToken(this, ticket);
            }
            break;
        }
    }

    private void onTicketIdClick(View view, TicketRange range) {
        Context context = view.getContext();

        //TODO: Perform some action when token is clicked
    }
}
