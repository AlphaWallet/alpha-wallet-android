package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.router.EthereumInfoRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.ui.widget.entity.ENSHandler;
import io.stormbird.wallet.ui.widget.entity.ItemClickListener;
import io.stormbird.wallet.viewmodel.Erc20DetailViewModel;
import io.stormbird.wallet.viewmodel.Erc20DetailViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;

import static io.stormbird.wallet.C.Key.WALLET;

public class Erc20DetailActivity extends BaseActivity implements Runnable, ItemClickListener {
    @Inject
    Erc20DetailViewModelFactory erc20DetailViewModelFactory;
    Erc20DetailViewModel viewModel;

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

    private ENSHandler ensHandler;
    Handler handler;

    //Token
    TextView balanceEth;
    TextView symbolText;
    TextView arrayBalance;
    TextView priceUSD;

    private LinearLayout valueDetailsLayout;
    private TextView usdValueText;
    private Button sendBtn;
    private Button receiveBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_erc20_token_detail);
        toolbar();
        setTitle("");

        viewModel = ViewModelProviders.of(this, erc20DetailViewModelFactory)
                .get(Erc20DetailViewModel.class);

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

        if (token.addressMatches(myAddress)) {
            viewModel.startEthereumTicker();
            viewModel.ethPriceReading().observe(this, this::onNewEthPrice);
        } else {
            //currently we don't evaluate ERC20 token value. TODO: Should we?
            valueDetailsLayout.setVisibility(View.GONE);
        }
    }

    private void onNewEthPrice(Double ethPrice) {
        usdValueText.setText(String.format("$%s", getUsdString(
                Double.valueOf(balanceEth.getText().toString()) * ethPrice)));
    }

    private void initViews() {
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
    public void run() {
        ensHandler.checkENS();
    }

    @Override
    public void onItemClick(String url) {
        ensHandler.handleHistoryItemClick(url);
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
    }
}
