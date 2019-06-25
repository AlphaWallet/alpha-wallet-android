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
import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.adapter.TicketSaleAdapter;
import io.stormbird.wallet.ui.widget.entity.TicketRangeParcel;
import io.stormbird.wallet.viewmodel.RedeemAssetSelectViewModel;
import io.stormbird.wallet.viewmodel.RedeemAssetSelectViewModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.List;

import static io.stormbird.wallet.C.Key.TICKET;
import static io.stormbird.wallet.C.Key.TICKET_RANGE;

/**
 * Created by James on 27/02/2018.
 */

/**
 * This is where we select indices to redeem
 */
public class RedeemAssetSelectActivity extends BaseActivity implements OnTokenClickListener
{
    @Inject
    protected RedeemAssetSelectViewModelFactory viewModelFactory;
    protected RedeemAssetSelectViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private int currentMenu = R.menu.send_menu;

    private FinishReceiver finishReceiver;

    public TextView ids;

    private Button nextButton;
    private Button redeemButton;

    private Token token;
    private TicketSaleAdapter adapter;
    private TicketRangeParcel ticketRange;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        token = getIntent().getParcelableExtra(TICKET);
        ticketRange = getIntent().getParcelableExtra(TICKET_RANGE);
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
        currentMenu = R.menu.send_menu;
        invalidateOptionsMenu();

        RecyclerView list = findViewById(R.id.listTickets);

        adapter = new TicketSaleAdapter(this, token, viewModel.getAssetDefinitionService());
        if (ticketRange != null)
        {
            adapter.setRedeemTicketQuantity(ticketRange.range, token);
            nextButton.setVisibility(View.GONE);
            redeemButton.setVisibility(View.VISIBLE);
            currentMenu = R.menu.redeem_menu;
            invalidateOptionsMenu();
        }
        else
        {
            adapter.setRedeemTicket(token);
            nextButton.setVisibility(View.VISIBLE);
            redeemButton.setVisibility(View.GONE);
        }
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
                finish();
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
            onTokenClick(null, token, range.tokenIds, true);

            adapter.setRedeemTicketQuantity(range, token);
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
            //form a new Ticket Range with the required indices to burn
            range.selectSubRange(quantity);
            viewModel.showRedeemSignature(this, range, token);
        }
    }

    @Override
    public void onTokenClick(View v, Token token, List<BigInteger> ids, boolean selected) {
        currentMenu = R.menu.redeem_menu;
        invalidateOptionsMenu();
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }
}
