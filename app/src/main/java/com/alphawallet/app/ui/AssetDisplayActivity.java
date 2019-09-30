package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.viewmodel.AssetDisplayViewModel;
import com.alphawallet.app.viewmodel.AssetDisplayViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;

/**
 * Created by James on 22/01/2018.
 */

/**
 *
 */
public class AssetDisplayActivity extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    protected AssetDisplayViewModelFactory assetDisplayViewModelFactory;
    private AssetDisplayViewModel viewModel;
    private SystemView systemView;
    private ProgressView progressView;
    private RecyclerView list;
    private FinishReceiver finishReceiver;
    private CertifiedToolbarView toolbarView;
    private FunctionButtonBar functionBar;
    private Token token;
    private NonFungibleTokenAdapter adapter;
    private String balance = null;
    private AWalletAlertDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        token = getIntent().getParcelableExtra(TICKET);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_display);
        toolbar();

        setTitle(getString(R.string.empty));
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.hide();
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::refreshAssets);
        
        list = findViewById(R.id.listTickets);
        toolbarView = findViewById(R.id.toolbar);

        viewModel = ViewModelProviders.of(this, assetDisplayViewModelFactory)
                .get(AssetDisplayViewModel.class);

        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ticket().observe(this, this::onTokenUpdate);
        viewModel.sig().observe(this, this::onSigData);

        functionBar = findViewById(R.id.layoutButtons);
        adapter = new NonFungibleTokenAdapter(functionBar, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService());
        functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, adapter);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.setHapticFeedbackEnabled(true);

        finishReceiver = new FinishReceiver(this);
        findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
        viewModel.checkTokenScriptValidity(token);
    }

    /**
     * Received Signature data either cached from AssetDefinitionService or from the API call
     * @param sigData
     */
    private void onSigData(XMLDsigDescriptor sigData)
    {
        toolbarView.onSigData(sigData);
        adapter.notifyItemChanged(0); //notify issuer update
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(token);
        if (functionBar == null) functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, adapter);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }

    private void onTokenUpdate(Token t)
    {
        if (t instanceof Ticket)
        {
            token = t;
            if (!t.getFullBalance().equals(balance))
            {
                adapter.setToken(token);
                RecyclerView list = findViewById(R.id.listTickets);
                list.setAdapter(null);
                list.setAdapter(adapter);
                balance = token.getFullBalance();
            }
        }
    }

    /**
     * Useful for volatile assets, this will refresh any volatile data in the token eg dynamic content or images
     */
    private void refreshAssets()
    {
        adapter.reloadAssets(this);
        systemView.hide();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_qr) {
            viewModel.showContractInfo(this, token);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void selectRedeemTokens(List<BigInteger> selection)
    {
        viewModel.selectRedeemTokens(this, token, selection);
    }

    @Override
    public void sellTicketRouter(List<BigInteger> selection)
    {
        viewModel.sellTicketRouter(this, token, token.intArrayToString(selection, false));
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        viewModel.showTransferToken(this, token, selection);
    }

    @Override
    public void displayTokenSelectionError(TSAction action)
    {
        if (dialog == null) dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.token_selection);
        dialog.setMessage(getString(R.string.token_requirement, String.valueOf(action.function.getTokenRequirement())));
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        //handle TS function
        Intent intent = new Intent(this, FunctionActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(WALLET, viewModel.defaultWallet().getValue());
        intent.putExtra(C.EXTRA_STATE, function);
        intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(adapter.getSelectedTokenIds(selection), true));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }
}
