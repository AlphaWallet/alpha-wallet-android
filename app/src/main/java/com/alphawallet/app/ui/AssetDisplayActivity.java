package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.viewmodel.AssetDisplayViewModel;
import com.alphawallet.app.viewmodel.AssetDisplayViewModelFactory;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;
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
 * This Activity shows the view-iconified breakdown for Non Fungible tokens
 * There's now some new timings here due to the way webView interacts with the Adapter listView
 *
 * If there's an asset definition for this NFT, we need to know the final height of the rendered webview before we start drawing the webviews on the adapter.
 * When a webview renders it first opens at 0 height, then springs open to a primary layout which is usually larger than required. Miliseconds later it collapses to the correct height.
 * Since a listview will usually respond to either the zero height or the primary layout height it is almost always renders incorrectly in a listview.
 *
 * What we do is make a (checked) assumption - all webviews on a ticket run will be the same height.
 * First render the first ticket in a drawing layer underneath the listview. Wait for rendering to finish; this will be the second layout update (see the layoutlistener)
 * We also need to ensure the view will time-out waiting to render and proceed as best it can without any pre-calculated height.
 *
 * After the height has been established we start the adapter and create all the views with the height pre-set to the calculated value.
 * Now everything renders correctly!
 *
 * Note that we need to pre-calculate both the view-iconified and views. If these are the same then an optimisation skips the normal view.
 *
 */
public class AssetDisplayActivity extends BaseActivity implements StandardFunctionInterface, PageReadyCallback, Runnable
{
    @Inject
    protected AssetDisplayViewModelFactory assetDisplayViewModelFactory;
    private AssetDisplayViewModel viewModel;
    private SystemView systemView;
    private ProgressBar progressView;
    private RecyclerView list;
    private FinishReceiver finishReceiver;
    private CertifiedToolbarView toolbarView;
    private FunctionButtonBar functionBar;
    private Token token;
    private NonFungibleTokenAdapter adapter;
    private AWalletAlertDialog dialog;
    private Web3TokenView testView;
    private final Handler handler = new Handler();
    private boolean iconifiedCheck;
    private int checkVal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        setContentView(R.layout.activity_asset_display);

        super.onCreate(savedInstanceState);

        toolbar();
        getIntents();
        setupSystemViews();
        setupViewModel();
        initView();

        finishReceiver = new FinishReceiver(this);
    }

    private void getIntents()
    {
        token = getIntent().getParcelableExtra(TICKET);
    }

    private void setupViewModel()
    {
        viewModel = ViewModelProviders.of(this, assetDisplayViewModelFactory)
                .get(AssetDisplayViewModel.class);

        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.ticket().observe(this, this::onTokenUpdate);
        viewModel.sig().observe(this, this::onSigData);
    }

    private void setupSystemViews()
    {
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.setVisibility(View.VISIBLE);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::refreshAssets);
    }

    private void initView()
    {
        testView = findViewById(R.id.test_web3);

        list = findViewById(R.id.listTickets);
        toolbarView = findViewById(R.id.toolbar);

        functionBar = findViewById(R.id.layoutButtons);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setHapticFeedbackEnabled(true);

        findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
        viewModel.checkTokenScriptValidity(token);
        setTitle(token.getShortName());

        iconifiedCheck = true;
        if (token.getArrayBalance().size() > 0 && viewModel.getAssetDefinitionService().hasDefinition(token.tokenInfo.chainId, token.tokenInfo.address) && token.iconifiedWebviewHeight == 0)
        {
            initWebViewCheck(iconifiedCheck);
            handler.postDelayed(this, 1500);
        }
        else
        {
            displayTokens();
        }
    }

    private void initWebViewCheck(boolean iconified)
    {
        checkVal = 0;
        //first see if we need this - is iconified equal to non iconified?
        if (!iconified && viewModel.getAssetDefinitionService().viewsEqual(token))
        {
            token.nonIconifiedWebviewHeight = token.iconifiedWebviewHeight;
        }
        else if (token.getArrayBalance().size() > 0)
        {
            BigInteger  tokenId = token.getArrayBalance().get(0);
            TicketRange data    = new TicketRange(tokenId, token.getAddress());
            token.renderTokenscriptView(data, viewModel.getAssetDefinitionService(), null, this, testView, iconified);
            testView.setOnReadyCallback(this);
        }
    }

    /**
     * Received Signature data either cached from AssetDefinitionService or from the API call
     * @param sigData
     */
    private void onSigData(XMLDsigDescriptor sigData)
    {
        toolbarView.onSigData(sigData, this);
        if (adapter != null) adapter.notifyItemChanged(0); //notify issuer update
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.prepare(token);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        getIntents();
        setupSystemViews();
        setupViewModel();
        initView();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
    }

    private void onTokenUpdate(Token t)
    {
        if (adapter != null && token.checkBalanceChange(t))
        {
            token = t;
            adapter.setToken(token);
            RecyclerView list = findViewById(R.id.listTickets);
            list.setAdapter(null);
            list.setAdapter(adapter);
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
        viewModel.sellTicketRouter(this, token, token.bigIntListToString(selection, false));
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
        intent.putExtra(C.EXTRA_TOKEN_ID, token.bigIntListToString(adapter.getSelectedTokenIds(selection), true));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }

    @Override
    public void onPageLoaded(WebView view)
    {
        testView.callToJS("refresh()");
    }

    @Override
    public void onPageRendered(WebView view)
    {
        testView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (token != null)
            {
                if (iconifiedCheck)token.iconifiedWebviewHeight = bottom - top;
                else token.nonIconifiedWebviewHeight = bottom - top;
                checkVal++;
                viewModel.setTokenViewDimensions(token);
            }

            if (checkVal == 3) addRunCall(0); //received the third webview render update - this is always the final size we want, but sometimes there's only 1 or 2 updates
            else addRunCall(400);//wait another 400ms for the second view update
        });
    }

    private void addRunCall(int delay)
    {
        handler.removeCallbacks(this);
        handler.postDelayed(this, delay);
    }

    @Override
    public void run()
    {
        if (iconifiedCheck)
        {
            iconifiedCheck = false;
            displayTokens();
            initWebViewCheck(iconifiedCheck);
        }
        else
        {
            //destroy webview
            testView.destroyDrawingCache();
            testView.removeAllViews();
            testView.loadUrl("about:blank");
            testView.setVisibility(View.GONE);
        }
    }

    private void displayTokens()
    {
        progressView.setVisibility(View.GONE);
        adapter = new NonFungibleTokenAdapter(functionBar, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService());
        functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, adapter);
        list.setAdapter(adapter);
    }
}
