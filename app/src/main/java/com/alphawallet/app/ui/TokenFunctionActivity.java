package com.alphawallet.app.ui;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.text.format.DateUtils;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModelFactory;
import com.alphawallet.app.web3.OnSetValuesListener;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActivityHistoryList;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.SystemView;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.ui.Erc20DetailActivity.HISTORY_LENGTH;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionActivity extends BaseActivity implements StandardFunctionInterface, PageReadyCallback, OnSetValuesListener
{
    @Inject
    protected TokenFunctionViewModelFactory tokenFunctionViewModelFactory;
    private TokenFunctionViewModel viewModel;

    private Web3TokenView tokenView;
    private Token token;
    private List<BigInteger> idList = null;
    private FunctionButtonBar functionBar;
    private final Map<String, String> args = new HashMap<>();
    private boolean reloaded;
    private AWalletAlertDialog dialog;
    private LinearLayout webWrapper;
    private ActivityHistoryList activityHistoryList = null;
    private Realm realm = null;
    private RealmResults<RealmToken> realmTokenUpdates;
    private void initViews(Token t) {
        token = t;
        String displayIds = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        tokenView = findViewById(R.id.web3_tokenview);
        webWrapper = findViewById(R.id.layout_webwrapper);
        idList = token.stringHexToBigIntegerList(displayIds);
        reloaded = false;

        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        tokenView.displayTicketHolder(token, data, viewModel.getAssetDefinitionService(), false);
        tokenView.setOnReadyCallback(this);
        tokenView.setOnSetValuesListener(this);

        activityHistoryList = findViewById(R.id.history_list);
        activityHistoryList.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_view);

        viewModel = new ViewModelProvider(this, tokenFunctionViewModelFactory)
                .get(TokenFunctionViewModel.class);
        viewModel.insufficientFunds().observe(this, this::errorInsufficientFunds);
        viewModel.invalidAddress().observe(this, this::errorInvalidAddress);
        viewModel.walletUpdate().observe(this, this::onWalletUpdate);

        SystemView systemView = findViewById(R.id.system_view);
        systemView.hide();
        functionBar = findViewById(R.id.layoutButtons);
        initViews(getIntent().getParcelableExtra(TICKET));
        toolbar();
        setTitle(getString(R.string.token_function));

        viewModel.startGasPriceUpdate(token.tokenInfo.chainId);
        viewModel.getCurrentWallet();
    }

    private void onWalletUpdate(Wallet w)
    {
        if(BuildConfig.DEBUG || viewModel.isAuthorizeToFunction())
        {
            functionBar.revealButtons();
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, idList);
            functionBar.setWalletType(w.type);
        }

        setupRealmListeners(w);
    }

    private void setupRealmListeners(Wallet w)
    {
        realm = viewModel.getRealmInstance(w);
        setTokenListener();
        setEventListener(w);
    }

    private void setTokenListener()
    {
        String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
        realmTokenUpdates = realm.where(RealmToken.class).equalTo("address", dbKey).findAllAsync();
        realmTokenUpdates.addChangeListener(realmTokens -> {
            if (realmTokens.size() == 0) return;
            RealmToken t = realmTokens.first();
            Token update = viewModel.getToken(t.getChainId(), t.getTokenAddress());
            if (update != null) initViews(update);
        });
    }

    private void setEventListener(Wallet wallet)
    {
        ActivityAdapter adapter = new ActivityAdapter(viewModel.getTokensService(), viewModel.getTransactionsInteract(),
                viewModel.getAssetDefinitionService());

        adapter.setDefaultWallet(wallet);

        activityHistoryList.setupAdapter(adapter);
        activityHistoryList.startActivityListeners(viewModel.getRealmInstance(wallet), wallet,
                token, viewModel.getTokensService(), idList.get(0), HISTORY_LENGTH);
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
        if (activityHistoryList != null) activityHistoryList.onDestroy();
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
        if (realm != null) realm.close();
    }

    @Override
    public void onPageLoaded(WebView view)
    {
        tokenView.callToJS("refresh()");
    }

    @Override
    public void onPageRendered(WebView view)
    {
        webWrapper.setVisibility(View.VISIBLE);
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
        viewModel.openUniversalLink(this, token, selection);
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        viewModel.showTransferToken(this, token, selection);
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
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        TSAction action = functions.get(function);
        if (action != null && action.view == null && action.function != null)
        {
            if (!viewModel.handleFunction(action, selection.get(0), token, this))
            {
                showTransactionError();
            }
        }
        else
        {
            viewModel.showFunction(this, token, function, idList);
        }
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

    @Override
    public void setValues(Map<String, String> updates)
    {
        boolean newValues = false;
        TicketRange data = new TicketRange(idList, token.tokenInfo.address, false);

        //called when values update
        for (String key : updates.keySet())
        {
            String value = updates.get(key);
            String old = args.put(key, updates.get(key));
            if (!value.equals(old)) newValues = true;
        }

        if (newValues)
        {
            viewModel.getAssetDefinitionService().addLocalRefs(args);
            //rebuild the view
            tokenView.displayTicketHolder(token, data, viewModel.getAssetDefinitionService(), false);
        }
    }

    private void showTransactionError()
    {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.tokenscript_error);
        dialog.setMessage(getString(R.string.invalid_parameters));
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v ->dialog.dismiss());
        dialog.show();
    }
}
