package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.router.EthereumInfoRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.ui.widget.adapter.TransactionsAdapter;
import io.stormbird.wallet.viewmodel.Erc20DetailViewModel;
import io.stormbird.wallet.viewmodel.Erc20DetailViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;

import static io.stormbird.wallet.C.Key.WALLET;

public class Erc20DetailActivity extends BaseActivity {
    @Inject
    Erc20DetailViewModelFactory erc20DetailViewModelFactory;
    Erc20DetailViewModel viewModel;

    private static final int HISTORY_LENGTH = 3;

    private boolean sendingTokens = false;
    private String myAddress;
    private int decimals;
    private String symbol;
    private Wallet wallet;
    private Token token;
    private String contractAddress;
    private double currentEthPrice;
    RelativeLayout ethDetailLayout;
    AWalletAlertDialog dialog;

    Handler handler;

    TextView balanceEth;
    TextView symbolText;
    TextView arrayBalance;
    TextView priceUSD;

    private LinearLayout valueDetailsLayout;
    private TextView usdValueText;
    private Button sendBtn;
    private Button receiveBtn;
    private RecyclerView list;
    private ProgressBar progressBar;

    private TransactionsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_erc20_token_detail);
        toolbar();
        setTitle("");

        viewModel = ViewModelProviders.of(this, erc20DetailViewModelFactory)
                .get(Erc20DetailViewModel.class);

        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.transactions().observe(this, this::onTransactions);

        handler = new Handler();

        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS); //contract address
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, C.ETHER_DECIMALS);
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        sendingTokens = getIntent().getBooleanExtra(C.EXTRA_SENDING_TOKENS, false);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        myAddress = wallet.address;

        setupTokenContent();

        initViews();

        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionsAdapter(this::onTransactionClick, viewModel.getTokensService(),
                viewModel.getTransactionsInteract(), R.layout.item_recent_transaction);

        list.setAdapter(adapter);

        if (token.addressMatches(myAddress)) {
            viewModel.startEthereumTicker();
            viewModel.ethPriceReading().observe(this, this::onNewEthPrice);
        } else {
            valueDetailsLayout.setVisibility(View.GONE);
        }
    }

    private void onTransactionClick(View view, Transaction transaction) {
        viewModel.showDetails(view.getContext(), transaction);
    }

    private void onTransactions(Transaction[] transactions) {
        progressBar.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);

        adapter.updateRecentTransactions(transactions, contractAddress, myAddress, HISTORY_LENGTH);
        adapter.notifyDataSetChanged();
    }

    private void onNewEthPrice(Double ethPrice) {
        usdValueText.setText(String.format("$%s", getUsdString(
                Double.valueOf(balanceEth.getText().toString()) * ethPrice)));
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress_bar);
        valueDetailsLayout = findViewById(R.id.layout_value_details);
        usdValueText = findViewById(R.id.usd_value);

        sendBtn = findViewById(R.id.button_send);
        sendBtn.setOnClickListener(v -> {
            new SendTokenRouter().open(this, myAddress, symbol, decimals, sendingTokens, wallet, token);

        });

        receiveBtn = findViewById(R.id.button_receive);
        receiveBtn.setOnClickListener(v -> {
            viewModel.showMyAddress(this, wallet);
        });

        ethDetailLayout = findViewById(R.id.layout_eth_detail);
        priceUSD = findViewById(R.id.textImportPriceUSD);
    }

    public void setupTokenContent() {
        balanceEth = findViewById(R.id.balance_eth);
        arrayBalance = findViewById(R.id.balanceArray);
        symbolText = findViewById(R.id.symbol);

        symbolText.setText(TextUtils.isEmpty(token.tokenInfo.name)
                ? token.tokenInfo.symbol.toUpperCase()
                : getString(R.string.token_name, token.tokenInfo.name, token.tokenInfo.symbol.toUpperCase()));

        TokenInfo tokenInfo = token.tokenInfo;
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? token.balance.divide(decimalDivisor) : token.balance;
        ethBalance = ethBalance.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        balanceEth.setText(value);

        balanceEth.setVisibility(View.VISIBLE);
        arrayBalance.setVisibility(View.GONE);

        if (viewModel.hasIFrame(token.getAddress())) {
            addTokenPage();
        }
    }

    private void addTokenPage() {
        LinearLayout viewWrapper = findViewById(R.id.layout_iframe);
        try {
            WebView iFrame = findViewById(R.id.iframe);
            String tokenData = viewModel.getTokenData(token.getAddress());
            iFrame.loadData(tokenData, "text/html", "UTF-8");
            viewWrapper.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            viewWrapper.setVisibility(View.GONE);
        }
    }

    public static String getUsdString(double usdPrice) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(usdPrice);
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
            case R.id.action_info: {
                new EthereumInfoRouter().open(this);
                break;
            }
            case R.id.action_qr:
                viewModel.showContractInfo(this, contractAddress);
                break;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        viewModel.cleanUp();
    }

    private void onDefaultWallet(Wallet wallet) {
        adapter.setDefaultWallet(wallet);
        viewModel.fetchTransactions(wallet, contractAddress);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        adapter.setDefaultNetwork(networkInfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
    }
}
