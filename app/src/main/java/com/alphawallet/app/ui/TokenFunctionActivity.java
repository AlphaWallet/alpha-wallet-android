package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;

import dagger.android.AndroidInjection;

import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.widget.ProgressView;
import com.alphawallet.app.widget.SystemView;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

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
    private StringBuilder attrs;
    private FunctionButtonBar functionBar;

    private void initViews() {
        token = getIntent().getParcelableExtra(TICKET);
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        RelativeLayout frameLayout = findViewById(R.id.layout_select_ticket);
        tokenView = findViewById(R.id.web3_tokenview);
        idList = token.stringHexToBigIntegerList(displayIds);

        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        token.displayTicketHolder(data, frameLayout, viewModel.getAssetDefinitionService(), this, false);
        functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null);
        functionBar.revealButtons();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view);

        viewModel = ViewModelProviders.of(this, tokenFunctionViewModelFactory)
                .get(TokenFunctionViewModel.class);
        SystemView systemView = findViewById(R.id.system_view);
        ProgressView progressView = findViewById(R.id.progress_view);
        systemView.hide();
        progressView.hide();
        functionBar = findViewById(R.id.layoutButtons);

        initViews();
        toolbar();
        setTitle(getString(R.string.token_function));

        //setupFunctions();
        viewModel.startGasPriceUpdate(token.tokenInfo.chainId);
        viewModel.getCurrentWallet();
    }

    @Override
    public void onResume()
    {
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
    public void onDestroy()
    {
        super.onDestroy();
        viewModel.stopGasSettingsFetch();
    }


    @Override
    public void onPageLoaded()
    {
        tokenView.callToJS("refresh()");
    }

    @Override
    public void selectRedeemTokens(List<BigInteger> selection)
    {
        viewModel.selectRedeemToken(this, token, idList);
    }

    @Override
    public void sellTicketRouter(List<BigInteger> selection)
    {
        viewModel.openUniversalLink(this, token, token.intArrayToString(idList, false));
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        viewModel.showTransferToken(this, token, token.intArrayToString(idList, false));
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
