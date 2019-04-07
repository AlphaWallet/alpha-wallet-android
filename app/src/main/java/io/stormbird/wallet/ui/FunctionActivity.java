package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.adapter.NonFungibleTokenAdapter;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModel;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import io.stormbird.wallet.web3.entity.FunctionCallback;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 4/04/2019.
 * Stormbird in Singapore
 */
public class FunctionActivity extends BaseActivity implements View.OnClickListener, FunctionCallback
{
    @Inject
    protected TokenFunctionViewModelFactory viewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Token token;
    private RecyclerView list;
    private NonFungibleTokenAdapter adapter;
    private String viewCode;
    private SystemView systemView;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        viewCode = getIntent().getStringExtra(C.EXTRA_STATE);
        list = findViewById(R.id.listTickets);
        adapter = new NonFungibleTokenAdapter(token, viewCode, this, viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.setHapticFeedbackEnabled(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_display);

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TokenFunctionViewModel.class);
        systemView = findViewById(R.id.system_view);
        ProgressView progressView = findViewById(R.id.progress_view);
        systemView.hide();
        progressView.hide();
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::updateView);

        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));
        setupFunctions();
    }

    private void updateView()
    {
        adapter.notifyDataSetChanged();
        systemView.hide();
    }

    private void setupFunctions()
    {
        Button[] buttons = new Button[3];
        buttons[0] = findViewById(R.id.button_use);
        buttons[1] = findViewById(R.id.button_sell);
        buttons[2] = findViewById(R.id.button_transfer);

        for (Button b : buttons)
        {
            b.setVisibility(View.GONE);
            b.setOnClickListener(this);
        }

        buttons[0].setVisibility(View.VISIBLE);
        buttons[0].setText("Confirm");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            //get challenge from the source
            case R.id.button_use:
                adapter.passFunction("onConfirm", "sig");
                break;
            default:
                break;
        }
    }

    @Override
    public void signMessage(byte[] sign, DAppFunction dAppFunction, Message<String> message)
    {
        viewModel.signMessage(sign, dAppFunction, message, token.tokenInfo.chainId, token.getWallet());
    }
}
