package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;

import dagger.android.AndroidInjection;

import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.List;

import static com.alphawallet.app.C.Key.TICKET;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionActivity extends BaseActivity implements StandardFunctionInterface, PageReadyCallback
{
    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Web3TokenView tokenView;
    private Token token;
    private List<BigInteger> idList = null;
    private FunctionButtonBar functionBar;
    private boolean reloaded;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view);

        getIntents();
        setupViewModel();
        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));
    }

    private void getIntents()
    {
        token = getIntent().getParcelableExtra(TICKET);
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        idList = token.stringHexToBigIntegerList(displayIds);
    }

    private void setupViewModel()
    {
        viewModel = ViewModelProviders.of(this, tokenFunctionViewModelFactory)
                .get(TokenFunctionViewModel.class);
        viewModel.startGasPriceUpdate(token.tokenInfo.chainId);
        viewModel.getCurrentWallet();
        viewModel.reloadScriptsIfRequired();
    }

    private void initViews()
    {
        RelativeLayout frameLayout = findViewById(R.id.layout_select_ticket);
        tokenView = findViewById(R.id.web3_tokenview);
        functionBar = findViewById(R.id.layoutButtons);

        reloaded = false;

        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        token.displayTicketHolder(data, frameLayout, viewModel.getAssetDefinitionService(), this, false);
        tokenView.setOnReadyCallback(this);
        functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null);
        functionBar.revealButtons();
        functionBar.setSelection(idList);

        SystemView systemView = findViewById(R.id.system_view);
        ProgressView progressView = findViewById(R.id.progress_view);
        systemView.hide();
        progressView.hide();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
        getIntents();
        setupViewModel();
        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));
    }

    @Override
    public void onStop()
    {
        super.onStop();
        viewModel.stopGasSettingsFetch();

        if (BuildConfig.BUILD_TYPE.equals("lifecycle_debug"))
        {
            //blank members
            viewModel.unloadScriptsForDebug();
            viewModel = null;
        }
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
    public void onPageLoaded(WebView view)
    {
        tokenView.callToJS("refresh()");
    }

    @Override
    public void onPageRendered(WebView view)
    {
        if (!reloaded) tokenView.reload(); //issue a single reload
        reloaded = true;
    }

    @Override
    public void selectRedeemTokens(List<BigInteger> selection)
    {
        viewModel.selectRedeemToken(this, token, selection);
    }

    @Override
    public void sellTicketRouter(List<BigInteger> selection)
    {
        viewModel.openUniversalLink(this, token, token.bigIntListToString(selection, false));
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        viewModel.showTransferToken(this, token, token.bigIntListToString(selection, false));
    }

    @Override
    public void showSend()
    {

    }

    @Override
    public void showReceive()
    {

    }

    @Override
    public void displayTokenSelectionError(TSAction action)
    {

    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        viewModel.showFunction(this, token, function, idList);
    }
}
