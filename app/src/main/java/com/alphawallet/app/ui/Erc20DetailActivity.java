package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.viewmodel.Erc20DetailViewModel;
import com.alphawallet.app.viewmodel.Erc20DetailViewModelFactory;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.repository.TransactionsRealmCache.convert;

public class Erc20DetailActivity extends BaseActivity implements StandardFunctionInterface
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
    private ProgressBar progressBar;
    private LinearLayout noTransactionsLayout;
    private TextView noTransactionsSubText;
    private RecyclerView tokenView;
    private RecyclerView recentTransactionsView;
    private CertifiedToolbarView toolbarView;

    private ActivityAdapter recentTransactionsAdapter;
    private TokensAdapter tokenViewAdapter;
    private Realm realm;
    private RealmResults<RealmToken> realmTokenUpdates;
    private RealmResults<RealmTransaction> realmTransactionUpdates;
    private long lastTxTimeStamp = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
            viewModel = ViewModelProviders.of(this, erc20DetailViewModelFactory)
                    .get(Erc20DetailViewModel.class);
            viewModel.transactions().observe(this, this::onTransactions);
            viewModel.sig().observe(this, sigData -> toolbarView.onSigData(sigData, this));
            viewModel.prepare(token, wallet);
            viewModel.newScriptFound().observe(this, this::onNewScript);
            findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
            viewModel.checkForNewScript(token);
        }

        if (realm == null)
        {
            realm = viewModel.getRealmInstance(wallet);
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
        if (recentTransactionsAdapter != null) return;
        recentTransactionsView = findViewById(R.id.list);
        recentTransactionsView.setLayoutManager(new LinearLayoutManager(this));
        recentTransactionsAdapter = new ActivityAdapter(this::onTransactionClick, viewModel.getTokensService(),
                viewModel.getTransactionsInteract(), R.layout.item_recent_transaction);
        recentTransactionsView.setAdapter(recentTransactionsAdapter);
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
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }

    private void getIntentData() {
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        tokenMeta = new TokenCardMeta(token.tokenInfo.chainId, token.getAddress(), token.balance.toString(), token.updateBlancaTime, token.lastTxCheck, token.getInterfaceSpec());
    }

    private void setTokenListener()
    {
        String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
        realmTokenUpdates = realm.where(RealmToken.class).equalTo("address", dbKey)
                .greaterThan("addedTime", System.currentTimeMillis()).findAllAsync();
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

    private void setTransactionListener()
    {
        realmTransactionUpdates = realm.where(RealmTransaction.class).equalTo("chainId", token.tokenInfo.chainId)
                .greaterThan("timeStamp", lastTxTimeStamp-60*10).findAllAsync();
        realmTransactionUpdates.addChangeListener(realmTransactions -> {
            List<TransactionMeta> metas = new ArrayList<>();
            if (realmTransactions.size() == 0) return;
            for (RealmTransaction item : realmTransactions)
            {
                if (convert(item).isRelated(token.getAddress(), wallet.address))
                {
                    boolean pendingTx = item.getBlockNumber().equals("0");
                    metas.add(new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), pendingTx));
                }
            }

            if (metas.size() > 0)
            {
                recentTransactionsAdapter.addNewTransactions(metas.toArray(new TransactionMeta[0]));
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

    private void onTransactionClick(View view, Transaction transaction) {
        viewModel.showDetails(this, wallet, transaction);
    }

    private void onTransactions(ActivityMeta[] transactions) {
        progressBar.setVisibility(View.GONE);
        recentTransactionsView.setVisibility(View.VISIBLE);

        if (transactions.length > 0)
        {
            recentTransactionsAdapter.addNewTransactions(transactions);
            noTransactionsLayout.setVisibility(View.GONE);
            recentTransactionsAdapter.notifyDataSetChanged();
            for (ActivityMeta a : transactions)
            {
                if (a.timeStamp > lastTxTimeStamp) lastTxTimeStamp = a.timeStamp;
            }
        }
        else if (recentTransactionsAdapter.getItemCount() == 0)
        {
            noTransactionsLayout.setVisibility(View.VISIBLE);
        }

        setTransactionListener();
    }

    private void initViews(Wallet wallet) {
        if (noTransactionsLayout != null) return;
        noTransactionsLayout = findViewById(R.id.layout_no_recent_transactions);
        noTransactionsSubText = findViewById(R.id.no_recent_transactions_subtext);

        if (token.addressMatches(wallet.address)) {
            noTransactionsSubText.setText(getString(R.string.no_recent_transactions_subtext,
                    getString(R.string.no_recent_transactions_subtext_ether)));
        } else {
            noTransactionsSubText.setText(getString(R.string.no_recent_transactions_subtext,
                    getString(R.string.no_recent_transactions_subtext_tokens)));
        }

        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.action_qr:
                viewModel.showContractInfo(this, wallet, token);
                break;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.getTokensService().clearFocusToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel == null)
        {
            getIntentData();
            setupViewModel();
            initViews(wallet);
            setUpTokenView();
            setUpRecentTransactionsView();
            recentTransactionsAdapter.setDefaultWallet(wallet);
        }
        viewModel.getTokensService().setFocusToken(token);
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
}
