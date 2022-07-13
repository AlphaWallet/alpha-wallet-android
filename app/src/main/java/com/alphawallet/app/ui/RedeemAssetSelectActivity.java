package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.ui.widget.entity.TicketRangeParcel;
import com.alphawallet.app.viewmodel.RedeemAssetSelectViewModel;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


import static com.alphawallet.app.C.Key.TICKET_RANGE;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by James on 27/02/2018.
 */

/**
 * This is where we select indices to redeem
 */
@AndroidEntryPoint
public class RedeemAssetSelectActivity extends BaseActivity implements TokensAdapterCallback
{
    protected RedeemAssetSelectViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private int currentMenu = R.menu.menu_send;

    private FinishReceiver finishReceiver;

    public TextView ids;

    private Button nextButton;
    private Button redeemButton;

    private Token token;
    private NonFungibleTokenAdapter adapter;
    private TicketRangeParcel ticketRange;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(RedeemAssetSelectViewModel.class);

        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
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

        setTitle(getString(R.string.empty));

        systemView = findViewById(R.id.system_view);
        systemView.hide();

        progressView = findViewById(R.id.progress_view);
        progressView.hide();

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        finishReceiver = new FinishReceiver(this);
        setupRedeemSelector();
    }

    private void setupRedeemSelector()
    {
        currentMenu = R.menu.menu_send;
        invalidateOptionsMenu();

        RecyclerView list = findViewById(R.id.listTickets);
        adapter = new NonFungibleTokenAdapter(this, token, ticketRange.range.tokenIds, viewModel.getAssetDefinitionService());
        adapter.addQuantitySelector();

        nextButton.setVisibility(View.GONE);
        redeemButton.setVisibility(View.VISIBLE);
        currentMenu = R.menu.menu_redeem;
        invalidateOptionsMenu();

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
        if (finishReceiver != null)
        {
            finishReceiver.unregister();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int action_next = R.id.action_next;
        final int action_redeem = R.id.action_redeem;
        switch (item.getItemId()) {
            case action_next: {
                onNext();
            }
            break;
            case action_redeem: {
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
    }

    private void onNext() {
        //first get range selection
        List<BigInteger> selection = adapter.getSelectedTokenIds(new ArrayList<>());// adapter.getCheckedItem();
        if (selection != null)
        {
            onTokenClick(null, token, selection, true);

            //adapter.setRedeemTicketQuantity(range, token);
            RecyclerView list = findViewById(R.id.listTickets);
            list.setAdapter(null);
            list.setAdapter(adapter);

            nextButton.setVisibility(View.GONE);
            redeemButton.setVisibility(View.VISIBLE);
        }
    }

    private void onRedeem()
    {
        TicketRange range = adapter.getSelectedRange(ticketRange.range.tokenIds);
        viewModel.showRedeemSignature(this, range, token);
    }

    @Override
    public void onTokenClick(View v, Token token, List<BigInteger> ids, boolean selected) {
        currentMenu = R.menu.menu_redeem;
        invalidateOptionsMenu();
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }
}
