package com.alphawallet.app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.viewmodel.SwapViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ConfirmSwapDialog;
import com.alphawallet.app.widget.SelectTokenDialog;
import com.alphawallet.app.widget.SwapSettingsDialog;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.app.widget.TokenSelector;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SwapActivity extends BaseActivity implements StandardFunctionInterface
{
    SwapViewModel viewModel;

    private TextView chainName;

    private TokenSelector sourceSelector;
    private TokenSelector destSelector;

    private SelectTokenDialog sourceTokenDialog;
    private SelectTokenDialog destTokenDialog;

    private ConfirmSwapDialog confirmSwapDialog;
    private SwapSettingsDialog settingsDialog;
    private AWalletAlertDialog progressDialog;

    private RelativeLayout tokenLayout;
    private LinearLayout infoLayout;
    private TokenInfoView fees;
    private TokenInfoView currentPrice;
    private TokenInfoView minReceived;
    private LinearLayout noConnectionsLayout;
    private MaterialButton continueBtn;
    private MaterialButton openSettingsBtn;

    private Token token;
    private Wallet wallet;

    private List<Chain> chains;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_swap);

        toolbar();

        setTitle("Swap");

        initViewModel();

        getIntentData();

        initViews();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(SwapViewModel.class);
        viewModel.error().observe(this, this::onError);
        viewModel.chains().observe(this, this::onChains);
        viewModel.chain().observe(this, this::onChain);
        viewModel.connections().observe(this, this::onConnections);
        viewModel.quote().observe(this, this::onQuote);
        viewModel.progress().observe(this, this::onProgress);
    }

    private void getIntentData()
    {
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
    }

    private void initViews()
    {
        chainName = findViewById(R.id.chain_name);
        sourceSelector = findViewById(R.id.from_input);
        destSelector = findViewById(R.id.to_input);
        tokenLayout = findViewById(R.id.layout_tokens);
        infoLayout = findViewById(R.id.layout_info);
        fees = findViewById(R.id.tiv_fees);
        currentPrice = findViewById(R.id.tiv_current_price);
        minReceived = findViewById(R.id.tiv_min_received);
        noConnectionsLayout = findViewById(R.id.layout_no_connections);
        continueBtn = findViewById(R.id.btn_continue);
        openSettingsBtn = findViewById(R.id.btn_open_settings);

        progressDialog = new AWalletAlertDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Fetching quote");
        progressDialog.setProgressMode();

        chainName.setOnClickListener(v -> {
            if (settingsDialog != null)
            {
                settingsDialog.show();
            }
        });

        continueBtn.setOnClickListener(v -> {
            if (confirmSwapDialog != null)
            {
                confirmSwapDialog.show();
            }
        });

        openSettingsBtn.setOnClickListener(v -> {
            if (settingsDialog != null)
            {
                settingsDialog.show();
            }
        });

        sourceSelector.setEventListener(new TokenSelector.TokenSelectorEventListener()
        {
            @Override
            public void onSelectorClicked()
            {
                sourceTokenDialog.show();
            }

            @Override
            public void onAmountChanged(String amount)
            {
                getQuote();
            }

            @Override
            public void onSelectionChanged(Connection.LToken token)
            {
                if (destSelector.getToken() == null)
                {
                    destSelector.setVisibility(View.VISIBLE);
                }

                sourceSelector.setBalance(viewModel.getBalance(wallet.address, token));

                infoLayout.setVisibility(View.GONE);

                getQuote();
            }

            @Override
            public void onMaxClicked()
            {
                String max = viewModel.getBalance(wallet.address, sourceSelector.getToken());
                if (!max.isEmpty())
                {
                    sourceSelector.setAmount(max);
                }
            }
        });

        destSelector.setEventListener(new TokenSelector.TokenSelectorEventListener()
        {
            @Override
            public void onSelectorClicked()
            {
                destTokenDialog.show();
            }

            @Override
            public void onAmountChanged(String amount)
            {
                // Do nothing; EditText is disabled for dest selector
            }

            @Override
            public void onSelectionChanged(Connection.LToken token)
            {
                destSelector.setBalance(viewModel.getBalance(wallet.address, token));

                infoLayout.setVisibility(View.GONE);

                getQuote();
            }

            @Override
            public void onMaxClicked()
            {
                // Do nothing; Max Button is not visible for dest selector.
            }
        });
    }

    private void confirmSwap(Quote quote)
    {
        confirmSwapDialog.dismiss();
        // TODO: Send Transaction?
        viewModel.sendTransaction(quote);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.getChains();
    }

    private void initSourceToken(List<Connection.LToken> fromTokens)
    {
        long networkId = fromTokens.get(0).chainId;

        String symbol = "eth";

        for (Chain c : chains)
        {
            if (c.id == networkId)
            {
                symbol = c.coin;
            }
        }

        boolean matchFound = false;

        for (Connection.LToken t : fromTokens)
        {
            if (t.symbol.equalsIgnoreCase(symbol))
            {
                sourceSelector.init(t);
                matchFound = true;
                break;
            }
        }

        if (!matchFound)
        {
            sourceSelector.reset();

            infoLayout.setVisibility(View.GONE);
        }
    }

    private void initFromDialog(List<Connection.LToken> fromTokens)
    {
        sourceTokenDialog = new SelectTokenDialog(fromTokens, this, tokenItem -> {
            sourceSelector.init(tokenItem);
            sourceTokenDialog.dismiss();
        });
    }

    private void initToDialog(List<Connection.LToken> toTokens)
    {
        destTokenDialog = new SelectTokenDialog(toTokens, this, tokenItem -> {
            destSelector.init(tokenItem);
            destTokenDialog.dismiss();
        });
    }

    private void getQuote()
    {
        continueBtn.setEnabled(false);

        if (sourceSelector.getToken() != null
                && destSelector.getToken() != null
                && !TextUtils.isEmpty(sourceSelector.getAmount()))
        {
            viewModel.getQuote(sourceSelector.getToken(), destSelector.getToken(), wallet.address, sourceSelector.getAmount(), settingsDialog.getSlippage());
        }
    }

    private void onChains(List<Chain> chains)
    {
        this.chains = chains;

        settingsDialog = new SwapSettingsDialog(this, chains,
                new SwapSettingsDialog.SwapSettingsInterface()
                {
                    @Override
                    public void onChainSelected(Chain chain)
                    {
                        chainName.setText(chain.name);
                        viewModel.setChain(chain);

                        sourceSelector.clear();
                        destSelector.clear();
                        viewModel.getConnections(chain.id, chain.id);
                        settingsDialog.dismiss();
                    }
                });

        // TODO Check default if selector is null
        if (sourceSelector.getToken() == null)
        {
            long id = token.tokenInfo.chainId;
            for (Chain c : chains)
            {
                if (id == c.id)
                {
                    viewModel.setChain(c);
                }
            }
        }
        else
        {
            long chainId = viewModel.getChain().id;
            viewModel.getConnections(chainId, chainId);
        }
    }

    private void onChain(Chain chain)
    {
        chainName.setText(chain.metamask.chainName);
        viewModel.getConnections(chain.id, chain.id);
        settingsDialog.setSelectedChain(chain.id);
    }

    private void onConnections(List<Connection> connections)
    {
        if (!connections.isEmpty())
        {
            List<Connection.LToken> fromTokens = new ArrayList<>();
            List<Connection.LToken> toTokens = new ArrayList<>();

            for (Connection c : connections)
            {
                for (Connection.LToken t : c.fromTokens)
                {
                    if (!fromTokens.contains(t))
                    {
                        t.balance = viewModel.getBalance(wallet.address, t);
                        fromTokens.add(t);
                    }
                }

                for (Connection.LToken t : c.toTokens)
                {
                    if (!toTokens.contains(t))
                    {
                        t.balance = viewModel.getBalance(wallet.address, t);
                        toTokens.add(t);
                    }
                }
            }

            initFromDialog(fromTokens);

            initToDialog(toTokens);

            initSourceToken(fromTokens);

            tokenLayout.setVisibility(View.VISIBLE);
            noConnectionsLayout.setVisibility(View.GONE);
        }
        else
        {
            tokenLayout.setVisibility(View.GONE);
            infoLayout.setVisibility(View.GONE);
            noConnectionsLayout.setVisibility(View.VISIBLE);
        }

    }

    private void onQuote(Quote quote)
    {
        updateDestAmount(quote);

        updateInfoSummary(quote);

        confirmSwapDialog = new ConfirmSwapDialog(this, quote, () -> confirmSwap(quote));

        continueBtn.setEnabled(true);
    }

    private void updateDestAmount(Quote quote)
    {
        String amount = quote.estimate.toAmountMin;
        long decimals = quote.action.toToken.decimals;

        String destAmount = BalanceUtils.getScaledValue(
                amount,
                decimals);

        destSelector.setAmount(destAmount);
    }

    private void updateInfoSummary(Quote quote)
    {
        fees.setValue("TODO"); // TODO

        BigDecimal s = new BigDecimal(quote.action.fromToken.priceUSD);
        BigDecimal d = new BigDecimal(quote.action.toToken.priceUSD);
        BigDecimal c = s.multiply(d);
        String currentPriceTxt = "1 " + quote.action.fromToken.symbol + " ≈ " + c.toString() + " " + quote.action.toToken.symbol;
        currentPrice.setValue(currentPriceTxt.trim());

        String minReceivedVal = BalanceUtils.getShortFormat(quote.estimate.toAmountMin, quote.action.toToken.decimals) + " " + quote.action.toToken.symbol;
        minReceived.setValue(minReceivedVal.trim());

        infoLayout.setVisibility(View.VISIBLE);
    }

    private void onProgress(Boolean shouldShowProgress)
    {
        if (shouldShowProgress)
        {
            progressDialog.show();
        }
        else if (progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        switch (errorEnvelope.code)
        {
            case C.ErrorCode.INSUFFICIENT_BALANCE:
                sourceSelector.setError(getString(R.string.error_insufficient_balance, sourceSelector.getToken().symbol));
                break;
            default:
                AWalletAlertDialog errorDialog = new AWalletAlertDialog(this);
                errorDialog.setTitle(R.string.title_dialog_error);
                errorDialog.setMessage(errorEnvelope.message);
                errorDialog.setButton(R.string.action_cancel, v -> errorDialog.dismiss());
                errorDialog.show();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_settings)
        {
            if (!chains.isEmpty())
            {
                settingsDialog.show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
