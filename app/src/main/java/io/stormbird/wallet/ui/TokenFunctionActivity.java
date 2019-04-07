package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import dagger.android.AndroidInjection;
import io.stormbird.token.entity.TSAction;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.adapter.NonFungibleTokenAdapter;
import io.stormbird.wallet.viewmodel.AssetDisplayViewModel;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModel;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionActivity extends BaseActivity implements View.OnClickListener, Runnable
{
    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Token token;
    private RecyclerView list;
    private NonFungibleTokenAdapter adapter;
    private Handler handler;
    private SystemView systemView;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        list = findViewById(R.id.listTickets);
        List<BigInteger> idList = token.stringHexToBigIntegerList(displayIds);
        adapter = new NonFungibleTokenAdapter(token, token.intArrayToString(idList, false), viewModel.getAssetDefinitionService());
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.setHapticFeedbackEnabled(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_display);

        viewModel = ViewModelProviders.of(this, tokenFunctionViewModelFactory)
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

        handler = new Handler();
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

        for (Button b : buttons) { b.setOnClickListener(this); }

        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.getAddress());
        if (functions != null)
        {
            int index = 0;
            for (String name : functions.keySet())
            {
                buttons[index].setText(name);
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        handler.postDelayed(this, 300);
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
            case R.id.button_use:
            {
                Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.getAddress());
                //this will be the user function
                String buttonText = ((Button)v).getText().toString();
                TSAction action = functions.get(buttonText);
                viewModel.showFunction(this, token, action.view);
            }
            break;
            case R.id.button_sell:
            {
                viewModel.sellTicketRouter(this, token);
            }
            break;
            case R.id.button_transfer:
            {
                viewModel.showTransferToken(this, token);
            }
            break;
        }
    }

    @Override
    public void run()
    {
        adapter.notifyDataSetChanged();
    }
}
