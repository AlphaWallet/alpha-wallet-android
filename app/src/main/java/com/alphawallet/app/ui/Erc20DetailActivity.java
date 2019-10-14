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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenTicker;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.adapter.TransactionsAdapter;

import dagger.android.AndroidInjection;

import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.token.entity.TSAction;

import com.alphawallet.app.C;
import com.alphawallet.app.R;

import com.alphawallet.app.viewmodel.Erc20DetailViewModel;
import com.alphawallet.app.viewmodel.Erc20DetailViewModelFactory;
import com.alphawallet.app.widget.CertifiedToolbarView;

import javax.inject.Inject;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;

public class Erc20DetailActivity extends BaseActivity implements StandardFunctionInterface
{
    @Inject
    Erc20DetailViewModelFactory erc20DetailViewModelFactory;
    Erc20DetailViewModel viewModel;

    private static final int HISTORY_LENGTH = 5;
    private static final int TX_CHECK_INTERVAL = 4; //check transactions every 4 intervals (or if balance update is seen).

    private boolean sendingTokens = false;
    private boolean hasDefinition = false;
    private String myAddress;
    private String symbol;
    private Wallet wallet;
    private Token token;

    private FunctionButtonBar functionBar;
    private ProgressBar progressBar;
    private LinearLayout noTransactionsLayout;
    private TextView noTransactionsSubText;
    private RecyclerView tokenView;
    private RecyclerView recentTransactionsView;
    private CertifiedToolbarView toolbarView;

    private TransactionsAdapter recentTransactionsAdapter;
    private TokensAdapter tokenViewAdapter;
    private int txUpdateCounter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc20_token_detail);
        toolbar();
        setTitle("");
        getIntentData();
        myAddress = wallet.address;
        txUpdateCounter = 0;
        toolbarView = findViewById(R.id.toolbar);

        viewModel = ViewModelProviders.of(this, erc20DetailViewModelFactory)
                .get(Erc20DetailViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.transactions().observe(this, this::onTransactions);
        viewModel.token().observe(this, this::onTokenData);
        viewModel.tokenTicker().observe(this, this::onTokenTicker);
        viewModel.transactionUpdate().observe(this, this::newTransactions);
        viewModel.sig().observe(this, toolbarView::onSigData);
    }

    private void onTokenTicker(Ticker ticker)
    {
        if (token != null && tokenViewAdapter != null) //might be a delayed return after user closed screen
        {
            token.ticker = new TokenTicker(ticker, token.getAddress(), null);
            //token.ticker = new TokenTicker(String.valueOf(token.tokenInfo.chainId), token.getAddress(), ticker.price_usd, ticker.percentChange24h, null);
            Token[] tokens = {token};
            tokenViewAdapter.setTokens(tokens);
            tokenViewAdapter.notifyDataSetChanged();
        }
    }

    private void newTransactions(Transaction[] transactions)
    {
        recentTransactionsAdapter.addTransactions(transactions);
    }

    private void setUpRecentTransactionsView() {
        recentTransactionsView = findViewById(R.id.list);
        recentTransactionsView.setLayoutManager(new LinearLayoutManager(this));
        recentTransactionsAdapter = new TransactionsAdapter(this::onTransactionClick, viewModel.getTokensService(),
                viewModel.getTransactionsInteract(), R.layout.item_recent_transaction);
        recentTransactionsView.setAdapter(recentTransactionsAdapter);
    }

    private void setUpTokenView() {
        tokenView = findViewById(R.id.token_view);
        tokenView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        tokenViewAdapter = new TokensAdapter(this, null, viewModel.getAssetDefinitionService());
        Token[] tokens = {token};
        tokenViewAdapter.setTokens(tokens);
        tokenView.setAdapter(tokenViewAdapter);

        setupButtons();

        findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE);
        viewModel.checkTokenScriptValidity(token);
    }

    private void setupButtons()
    {
        if (wallet.type != WalletType.WATCH)
        {
            functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null);
            functionBar.revealButtons();
        }
    }

    private void getIntentData() {
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        sendingTokens = getIntent().getBooleanExtra(C.EXTRA_SENDING_TOKENS, false);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        hasDefinition = getIntent().getBooleanExtra(C.EXTRA_HAS_DEFINITION, false);
    }

    private void onTokenData(Token tokenUpdate)
    {
        if (token == null || tokenUpdate == null) return;
        tokenViewAdapter.clear();

        if ((txUpdateCounter%TX_CHECK_INTERVAL) == 0 || token.checkBalanceChange(tokenUpdate))
        {
            //trigger transaction fetch
            if (tokenUpdate.balanceIncrease(token)) playNotification();
            viewModel.fetchNetworkTransactions(token, HISTORY_LENGTH);
        }

        token = tokenUpdate;
        Token[] tokens = {token};
        tokenViewAdapter.setTokens(tokens);
        tokenViewAdapter.notifyDataSetChanged();
        txUpdateCounter++;
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
        viewModel.showDetails(this, transaction);
    }

    private void onTransactions(Transaction[] transactions) {
        progressBar.setVisibility(View.GONE);
        recentTransactionsView.setVisibility(View.VISIBLE);

        if (transactions.length > 0)
        {
            int txCount = recentTransactionsAdapter.updateRecentTransactions(transactions);
            noTransactionsLayout.setVisibility(View.GONE);
            recentTransactionsAdapter.notifyDataSetChanged();
        }
        else if (recentTransactionsAdapter.getItemCount() == 0)
        {
            noTransactionsLayout.setVisibility(View.VISIBLE);
        }
    }

    private void initViews(Wallet wallet) {
        noTransactionsLayout = findViewById(R.id.layout_no_recent_transactions);
        noTransactionsSubText = findViewById(R.id.no_recent_transactions_subtext);

        if (token.addressMatches(myAddress)) {
            noTransactionsSubText.setText(getString(R.string.no_recent_transactions_subtext,
                    getString(R.string.no_recent_transactions_subtext_ether)));
        } else {
            noTransactionsSubText.setText(getString(R.string.no_recent_transactions_subtext,
                    getString(R.string.no_recent_transactions_subtext_tokens)));
        }

        progressBar = findViewById(R.id.progress_bar);
    }

    private void addTokenPage() {
        LinearLayout viewWrapper = findViewById(R.id.layout_iframe);
        try {
            WebView iFrame = findViewById(R.id.iframe);
            String tokenData = viewModel.getTokenData(token);
            iFrame.loadData(tokenData, "text/html", "UTF-8");
            viewWrapper.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            viewWrapper.setVisibility(View.GONE);
        }
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
                viewModel.showContractInfo(this, token);
                break;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.cleanUp();
    }

    private void onDefaultWallet(Wallet wallet) {
        initViews(wallet);
        setUpTokenView();
        setUpRecentTransactionsView();
        recentTransactionsAdapter.setDefaultWallet(wallet);
        viewModel.updateDefaultBalance(token);
        viewModel.fetchTransactions(token, HISTORY_LENGTH);
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop updates
        viewModel.cleanUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(token);
        viewModel.startEthereumTicker(token);
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
        viewModel.showSendToken(this, myAddress, token);
    }

    @Override
    public void showReceive()
    {
        viewModel.showMyAddress(this, wallet, token);
    }
}
