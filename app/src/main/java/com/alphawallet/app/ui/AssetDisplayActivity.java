package com.alphawallet.app.ui;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.viewmodel.AdvancedSettingsViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActivityHistoryList;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.ui.Erc20DetailActivity.HISTORY_LENGTH;

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
    private static final int TOKEN_SIZING_DELAY = 3000; //3 seconds until timeout waiting for tokenview size calculation

    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
    private TokenFunctionViewModel viewModel;

    private SystemView systemView;
    private ProgressBar progressView;
    private FinishReceiver finishReceiver;
    private CertifiedToolbarView toolbarView;
    private FunctionButtonBar functionBar;
    private Token token;
    private Wallet wallet;
    private NonFungibleTokenAdapter adapter;
    private AWalletAlertDialog dialog;
    private Web3TokenView testView;
    private final Handler handler = new Handler();
    private int checkVal;
    private int itemViewHeight;

    private RecyclerView tokenView;
    private ActivityHistoryList activityHistoryList = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        token = getIntent().getParcelableExtra(TICKET);
        wallet = getIntent().getParcelableExtra(WALLET);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_display);
        toolbar();

        setTitle(token.getShortName());
        systemView = findViewById(R.id.system_view);
        systemView.hide();
        progressView = findViewById(R.id.progress_view);
        progressView.setVisibility(View.VISIBLE);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        refreshLayout.setOnRefreshListener(this::refreshAssets);

        testView = findViewById(R.id.test_web3);

        tokenView = findViewById(R.id.token_view);
        toolbarView = findViewById(R.id.toolbar);

        viewModel = new ViewModelProvider(this, tokenFunctionViewModelFactory)
                .get(TokenFunctionViewModel.class);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.sig().observe(this, this::onSigData);
        viewModel.insufficientFunds().observe(this, this::errorInsufficientFunds);
        viewModel.invalidAddress().observe(this, this::errorInvalidAddress);
        viewModel.newScriptFound().observe(this, this::onNewScript);

        functionBar = findViewById(R.id.layoutButtons);

        tokenView.setLayoutManager(new LinearLayoutManager(this));
        tokenView.setHapticFeedbackEnabled(true);

        finishReceiver = new FinishReceiver(this);
        findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
        viewModel.checkTokenScriptValidity(token);
        token.clearResultMap();

        if (token.getArrayBalance().size() > 0 && viewModel.getAssetDefinitionService().hasDefinition(token.tokenInfo.chainId, token.tokenInfo.address))
        {
            loadItemViewHeight();
        }
        else
        {
            displayTokens();
        }

        viewModel.checkForNewScript(token); //check for updated script
        setUpRecentTransactionsView();
        viewModel.updateTokensCheck(token);
    }

    private void loadItemViewHeight()
    {
        viewModel.getAssetDefinitionService().fetchViewHeight(token.tokenInfo.chainId, token.getAddress())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::viewHeight, error -> { viewHeight(0); })
                .isDisposed();
    }

    private void viewHeight(int fetchedViewHeight)
    {
        if (fetchedViewHeight < 100)
        {
            initWebViewCheck();
            handler.postDelayed(this, TOKEN_SIZING_DELAY); //wait 3 seconds until ending height check
        }
        else
        {
            token.itemViewHeight = fetchedViewHeight;
            displayTokens();
        }
    }

    private void onNewScript(Boolean aBoolean)
    {
        //need to reload tokens, now we have an updated/new script
        if (viewModel.getAssetDefinitionService().hasDefinition(token.tokenInfo.chainId, token.tokenInfo.address))
        {
            initWebViewCheck();
            handler.postDelayed(this, TOKEN_SIZING_DELAY);
        }
    }

    private void initWebViewCheck()
    {
        checkVal = 0;
        itemViewHeight = 0;
        //first see if we need this - is iconified equal to non iconified?
        if (token.getArrayBalance().size() > 0)
        {
            BigInteger  tokenId = token.getArrayBalance().get(0);
            TicketRange data    = new TicketRange(tokenId, token.getAddress());
            testView.renderTokenscriptView(token, data, viewModel.getAssetDefinitionService(), true);
            testView.setOnReadyCallback(this);
        }
        else
        {
            displayTokens();
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
        viewModel.prepare();
        if (functionBar == null)
        {
            functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, adapter, token.getArrayBalance());
            functionBar.setWalletType(wallet.type);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(finishReceiver);
        viewModel.clearFocusToken();
        if (adapter != null) adapter.onDestroy(tokenView);
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
        getMenuInflater().inflate(R.menu.menu_show_contract, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_show_contract) {
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
        viewModel.sellTicketRouter(this, token, selection);
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
    public void showWaitSpinner(boolean show)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (!show) return;
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(getString(R.string.check_function_availability));
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setProgressMode();
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void handleFunctionDenied(String denialMessage)
    {
        if (dialog == null) dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.token_selection);
        dialog.setMessage(denialMessage);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        //does the function have a view? If it's transaction only then handle here
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(function);
        token.clearResultMap();

        //handle TS function
        if (action != null && action.view == null && action.function != null)
        {
            //go straight to function call
            viewModel.handleFunction(action, selection.get(0), token, this);
        }
        else
        {
            viewModel.showFunction(this, token, function, selection);
        }
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
            itemViewHeight = bottom - top;
            checkVal++;
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
        token.itemViewHeight = itemViewHeight;
        viewModel.updateTokenScriptViewSize(token, itemViewHeight);
        displayTokens();
        //destroy webview
        testView.destroyDrawingCache();
        testView.removeAllViews();
        testView.loadUrl("about:blank");
        testView.setVisibility(View.GONE);
    }

    private void displayTokens()
    {
        handler.removeCallbacks(this);
        progressView.setVisibility(View.GONE);
        adapter = new NonFungibleTokenAdapter(functionBar, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService(), this);
        functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, adapter, token.getArrayBalance());
        functionBar.setWalletType(wallet.type);
        tokenView.setAdapter(adapter);
    }

    public void storeAsset(Asset asset)
    {
        viewModel.getTokensService().storeAsset(token, asset);
    }

    private void errorInsufficientFunds(Token currency)
    {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_insufficient_funds);
        dialog.setMessage(getString(R.string.current_funds, currency.getCorrectedBalance(currency.tokenInfo.decimals), currency.getSymbol()));
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void errorInvalidAddress(String address)
    {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.error_invalid_address);
        dialog.setMessage(getString(R.string.invalid_address_explain, address));
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setUpRecentTransactionsView()
    {
        if (activityHistoryList != null) return;
        activityHistoryList = findViewById(R.id.history_list);
        ActivityAdapter adapter = new ActivityAdapter(viewModel.getTokensService(), viewModel.getTransactionsInteract(),
                viewModel.getAssetDefinitionService());

        adapter.setDefaultWallet(wallet);

        activityHistoryList.setupAdapter(adapter);
        activityHistoryList.startActivityListeners(viewModel.getRealmInstance(wallet), wallet,
                token, viewModel.getTokensService(), BigInteger.ZERO, HISTORY_LENGTH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case C.TERMINATE_ACTIVITY:
                if (resultCode == RESULT_OK && data != null)
                {
                    Intent i = new Intent();
                    i.putExtra(C.EXTRA_TXHASH, data.getStringExtra(C.EXTRA_TXHASH));
                    setResult(RESULT_OK, new Intent());
                    finish();
                }
                break;
            default:
                break;
        }
    }
}
