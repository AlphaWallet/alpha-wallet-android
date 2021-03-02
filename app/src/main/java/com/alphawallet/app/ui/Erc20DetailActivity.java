package com.alphawallet.app.ui;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.BuyCryptoInterface;
import com.alphawallet.app.entity.OnRampContract;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.viewmodel.Erc20DetailViewModel;
import com.alphawallet.app.viewmodel.Erc20DetailViewModelFactory;
import com.alphawallet.app.widget.ActivityHistoryList;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;

public class Erc20DetailActivity extends BaseActivity implements StandardFunctionInterface, BuyCryptoInterface
{
    @Inject
    Erc20DetailViewModelFactory erc20DetailViewModelFactory;
    Erc20DetailViewModel viewModel;

    public static final int HISTORY_LENGTH = 5;

    private String symbol;
    private Wallet wallet;
    private Token token;
    private TokenCardMeta tokenMeta;

    private FunctionButtonBar functionBar;
    private RecyclerView tokenView;
    private CertifiedToolbarView toolbarView;

    private TokensAdapter tokenViewAdapter;
    private ActivityHistoryList activityHistoryList = null;
    private Realm realm = null;
    private RealmResults<RealmToken> realmTokenUpdates;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc20_token_detail);
        toolbar();
        setTitle("");
    }

    private void setupViewModel()
    {
        toolbarView = findViewById(R.id.toolbar);

        if (viewModel == null)
        {
            viewModel = new ViewModelProvider(this, erc20DetailViewModelFactory)
                    .get(Erc20DetailViewModel.class);
            viewModel.sig().observe(this, sigData -> toolbarView.onSigData(sigData, this));
            viewModel.newScriptFound().observe(this, this::onNewScript);
            findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
            viewModel.checkForNewScript(token);
        }
    }

    private void onNewScript(Boolean hasNewScript)
    {
        //found a new tokenscript for this token, create a new meta with balance set to trigger view update; view will update the token name
        tokenViewAdapter.updateToken(new TokenCardMeta(token.tokenInfo.chainId, token.getAddress(), "force_update",
                token.updateBlancaTime, token.lastTxCheck, token.getInterfaceSpec()), true);
        viewModel.checkTokenScriptValidity(token); //check script signature
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
                token, BigInteger.ZERO, HISTORY_LENGTH);
    }

    private void setUpTokenView()
    {
        if (tokenViewAdapter != null) return;
        tokenView = findViewById(R.id.token_view);
        tokenView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        tokenViewAdapter = new TokensAdapter(null, viewModel.getAssetDefinitionService(), viewModel.getTokensService(), this);
        tokenViewAdapter.updateToken(tokenMeta, true);
        tokenViewAdapter.setDebug();
        tokenView.setAdapter(tokenViewAdapter);
        setTokenListener();
        setupButtons();
        viewModel.checkTokenScriptValidity(token);
    }

    private void setupButtons()
    {
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupBuyFunction(this, viewModel.getOnRampRepository());
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }

    private void getIntentData()
    {
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        tokenMeta = new TokenCardMeta(token);
    }

    private void setTokenListener()
    {
        if (realm == null) realm = viewModel.getRealmInstance(wallet);
        String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
        realmTokenUpdates = realm.where(RealmToken.class).equalTo("address", dbKey)
                .greaterThan("addedTime", System.currentTimeMillis()- 5 * DateUtils.MINUTE_IN_MILLIS).findAllAsync();
        realmTokenUpdates.addChangeListener(realmTokens -> {
            if (realmTokens.size() == 0) return;
            for (RealmToken t : realmTokens)
            {
                TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), t.getBalance(),
                        t.getUpdateTime(), t.getLastTxTime(), t.getContractType());

                if (!tokenMeta.balance.equals(meta.balance))
                {
                    playNotification();
                    tokenMeta = meta;
                }

                tokenViewAdapter.updateToken(meta, true);
            }
        });
    }

    private void playNotification()
    {
        try
        {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(this, notification);
            r.play();
        }
        catch (Exception e)
        {
            //empty
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
        }
        else if (item.getItemId() == R.id.action_qr)
        {
            viewModel.showContractInfo(this, wallet, token);
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activityHistoryList != null) activityHistoryList.onDestroy();
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
        if (tokenViewAdapter != null && tokenView != null) tokenViewAdapter.onDestroy(tokenView);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.getTokensService().clearFocusToken();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (viewModel == null)
        {
            getIntentData();
            setupViewModel();
            setUpTokenView();
            setUpRecentTransactionsView();
        }
        viewModel.getTokensService().setFocusToken(token);
        viewModel.restartServices();
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        Intent intent = new Intent(this, FunctionActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(C.EXTRA_STATE, function);
        intent.putExtra(C.EXTRA_TOKEN_ID, BigInteger.ZERO.toString(16));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }

    @Override
    public void showSend()
    {
        viewModel.showSendToken(this, wallet, token);
    }

    @Override
    public void showReceive()
    {
        viewModel.showMyAddress(this, wallet, token);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        String transactionHash = null;

        switch (requestCode)
        {
            case C.COMPLETED_TRANSACTION: //completed a transaction send and got with either a hash or a null
                if (data != null) transactionHash = data.getStringExtra("tx_hash");
                if (transactionHash != null)
                {
                    //display transaction complete message

                }
                break;
        }
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (actionId == R.string.convert_to_xdai)
        {
            openDapp(C.XDAI_BRIDGE_DAPP);
        }
    }

    private void openDapp(String dappURL)
    {
        //switch to dappbrowser and open at dappURL
        Intent intent = new Intent();
        intent.putExtra(C.DAPP_URL_LOAD, dappURL);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void handleBuyFunction(Token token)
    {
        Intent intent = viewModel.getBuyIntent(wallet.address, token);
        setResult(RESULT_OK, intent);
        finish();
    }
}
