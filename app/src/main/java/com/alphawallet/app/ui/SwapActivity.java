package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.viewmodel.SwapViewModel;
import com.alphawallet.app.viewmodel.Tokens;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.SelectTokenDialog;
import com.alphawallet.app.widget.SwapSettingsDialog;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.app.widget.TokenSelector;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.tools.Numeric;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SwapActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
    private static final int GET_QUOTE_INTERVAL_MS = 30000;
    private SwapViewModel viewModel;
    private TokenSelector sourceSelector;
    private TokenSelector destSelector;
    private SelectTokenDialog sourceTokenDialog;
    private SelectTokenDialog destTokenDialog;
    private ActionSheetDialog confirmationDialog;
    private SwapSettingsDialog settingsDialog;
    private AWalletAlertDialog progressDialog;
    private AWalletAlertDialog errorDialog;
    private RelativeLayout tokenLayout;
    private LinearLayout infoLayout;
    private TokenInfoView provider;
    private TokenInfoView fees;
    private TokenInfoView currentPrice;
    private TokenInfoView minReceived;
    private LinearLayout noConnectionsLayout;
    private MaterialButton continueBtn;
    private MaterialButton openSettingsBtn;
    private TextView chainName;
    private Token token;
    private Wallet wallet;
    private Connection.LToken sourceToken;
    private List<Chain> chains;
    private final Handler getQuoteHandler = new Handler();
    private final Runnable getQuoteRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            viewModel.getQuote(
                    sourceSelector.getToken(),
                    destSelector.getToken(),
                    wallet.address,
                    sourceSelector.getAmount(),
                    settingsDialog.getSlippage());
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_swap);

        toolbar();

        setTitle(getString(R.string.swap));

        initViewModel();

        getIntentData();

        initViews();

        viewModel.getChains();
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
        viewModel.progressInfo().observe(this, this::onProgressInfo);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);
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
        provider = findViewById(R.id.tiv_provider);
        fees = findViewById(R.id.tiv_fees);
        currentPrice = findViewById(R.id.tiv_current_price);
        minReceived = findViewById(R.id.tiv_min_received);
        noConnectionsLayout = findViewById(R.id.layout_no_connections);
        continueBtn = findViewById(R.id.btn_continue);
        openSettingsBtn = findViewById(R.id.btn_open_settings);

        progressDialog = new AWalletAlertDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setProgressMode();

        chainName.setOnClickListener(v -> {
            if (settingsDialog != null)
            {
                settingsDialog.show();
            }
        });

        continueBtn.setOnClickListener(v -> {
            showConfirmDialog();
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
                startQuoteTask();
            }

            @Override
            public void onSelectionChanged(Connection.LToken token)
            {
                sourceTokenChanged(token);
            }

            @Override
            public void onMaxClicked()
            {
                String max = viewModel.getBalance(sourceSelector.getToken());
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
                destTokenChanged(token);
            }

            @Override
            public void onMaxClicked()
            {
                // Do nothing; Max Button is not visible for dest selector.
            }
        });
    }

    private void showConfirmDialog()
    {
        if (confirmationDialog != null && !confirmationDialog.isShowing())
        {
            confirmationDialog.show();
            confirmationDialog.fullExpand();
        }
    }

    private ActionSheetDialog createConfirmationAction(Quote quote)
    {
        ActionSheetDialog confDialog = null;
        try
        {
            Token activeToken = viewModel.getTokensService().getTokenOrBase(sourceToken.chainId, sourceToken.address);
            Web3Transaction w3Tx = viewModel.buildWeb3Transaction(quote);
            confDialog = new ActionSheetDialog(this, w3Tx, activeToken,
                    "", w3Tx.recipient.toString(), viewModel.getTokensService(), this);
            confDialog.setURL(quote.toolDetails.name);
            confDialog.setCanceledOnTouchOutside(false);
            confDialog.setGasEstimate(Numeric.toBigInt(quote.transactionRequest.gasLimit));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return confDialog;
    }

    private void destTokenChanged(Connection.LToken token)
    {
        destSelector.setBalance(viewModel.getBalance(token));

        infoLayout.setVisibility(View.GONE);

        destTokenDialog.setSelectedToken(token.address);

        startQuoteTask();
    }

    private void sourceTokenChanged(Connection.LToken token)
    {
        if (destSelector.getToken() == null)
        {
            destSelector.setVisibility(View.VISIBLE);
        }

        sourceSelector.clearAmount();

        destSelector.clearAmount();

        sourceSelector.setBalance(viewModel.getBalance(token));

        infoLayout.setVisibility(View.GONE);

        sourceTokenDialog.setSelectedToken(token.address);

        sourceToken = token;

        startQuoteTask();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    // The source token should default to the token selected in the main wallet dialog (ie the token from the intent).
    private void initSourceToken(Connection.LToken selectedToken)
    {
        if (selectedToken != null)
        {
            sourceSelector.init(selectedToken);
            sourceTokenChanged(selectedToken);
        }
        else
        {
            sourceSelector.reset();
            infoLayout.setVisibility(View.GONE);
        }
    }

    private void initFromDialog(List<Connection.LToken> fromTokens)
    {
        Tokens.sortValue(fromTokens);
        sourceTokenDialog = new SelectTokenDialog(fromTokens, this, tokenItem -> {
            sourceSelector.init(tokenItem);
            sourceTokenDialog.dismiss();
        });
    }

    private void initToDialog(List<Connection.LToken> toTokens)
    {
        Tokens.sortName(toTokens);
        Tokens.sortValue(toTokens);
        destTokenDialog = new SelectTokenDialog(toTokens, this, tokenItem -> {
            destSelector.init(tokenItem);
            destTokenDialog.dismiss();
        });
    }

    private void startQuoteTask()
    {
        stopQuoteTask();

        continueBtn.setEnabled(false);

        if (sourceSelector.getToken() != null
                && destSelector.getToken() != null
                && !TextUtils.isEmpty(sourceSelector.getAmount()))
        {
            getQuoteHandler.post(getQuoteRunnable);
        }
    }

    private void stopQuoteTask()
    {
        getQuoteHandler.removeCallbacks(getQuoteRunnable);
    }

    private void onChains(List<Chain> chains)
    {
        this.chains = chains;

        settingsDialog = new SwapSettingsDialog(this, chains,
                chain -> {
                    chainName.setText(chain.name);
                    viewModel.setChain(chain);

                    sourceSelector.clear();
                    destSelector.clear();
                    viewModel.getConnections(chain.id, chain.id);
                    settingsDialog.dismiss();
                });

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
            Connection.LToken selectedToken = null;

            for (Connection c : connections)
            {
                for (Connection.LToken t : c.fromTokens)
                {
                    if (!fromTokens.contains(t))
                    {
                        t.balance = viewModel.getBalance(t);
                        t.fiatEquivalent = viewModel.getFiatValue(t);

                        if (t.fiatEquivalent > 0)
                        {
                            fromTokens.add(t);

                            if (t.chainId == token.tokenInfo.chainId && t.address.equalsIgnoreCase(token.getAddress()))
                            {
                                selectedToken = t;
                            }
                        }
                    }
                }

                for (Connection.LToken t : c.toTokens)
                {
                    if (!toTokens.contains(t))
                    {
                        t.balance = viewModel.getBalance(t);
                        t.fiatEquivalent = viewModel.getFiatValue(t);
                        toTokens.add(t);
                    }
                }
            }

            initFromDialog(fromTokens);

            initToDialog(toTokens);

            initSourceToken(selectedToken);

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

        if (confirmationDialog == null || !confirmationDialog.isShowing())
        {
            confirmationDialog = createConfirmationAction(quote);
        }

        continueBtn.setEnabled(true);

        getQuoteHandler.postDelayed(getQuoteRunnable, GET_QUOTE_INTERVAL_MS);
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
        provider.setValue(quote.toolDetails.name);

        StringBuilder total = new StringBuilder();
        for (Quote.Estimate.GasCost gc : quote.estimate.gasCosts)
        {
            BigDecimal amount = new BigDecimal(gc.amount);
            long decimals = gc.token.decimals;
            String fee = BalanceUtils.getScaledValueFixed(amount, decimals, 4);
            total.append(fee).append(" ").append(gc.token.symbol).append("\n");
        }
        fees.setValue(total.toString().trim());

        BigDecimal s = new BigDecimal(quote.action.fromToken.priceUSD);
        BigDecimal d = new BigDecimal(quote.action.toToken.priceUSD);
        BigDecimal c = s.multiply(d);
        String currentPriceTxt = "1 " + quote.action.fromToken.symbol + " ≈ " + c.toString() + " " + quote.action.toToken.symbol;
        currentPrice.setValue(currentPriceTxt.trim());

        String minReceivedVal = BalanceUtils.getShortFormat(quote.estimate.toAmountMin, quote.action.toToken.decimals) + " " + quote.action.toToken.symbol;
        minReceived.setValue(minReceivedVal.trim());

        infoLayout.setVisibility(View.VISIBLE);
    }

    private void onProgressInfo(int code)
    {
        String message;
        switch (code)
        {
            case C.ProgressInfo.FETCHING_CHAINS:
                message = getString(R.string.message_fetching_chains);
                break;
            case C.ProgressInfo.FETCHING_CONNECTIONS:
                message = getString(R.string.message_fetching_connections);
                break;
            case C.ProgressInfo.FETCHING_QUOTE:
                message = getString(R.string.message_fetching_quote);
                break;
            default:
                message = getString(R.string.title_dialog_handling);
                break;
        }
        progressDialog.setTitle(message);
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

    private void txWritten(TransactionData transactionData)
    {
        AWalletAlertDialog successDialog = new AWalletAlertDialog(this);
        successDialog.setTitle(R.string.transaction_succeeded);
        successDialog.setMessage(transactionData.txHash);
        successDialog.show();
    }

    private void txError(Throwable throwable)
    {
        AWalletAlertDialog errorDialog = new AWalletAlertDialog(this);
        errorDialog.setTitle(R.string.error_transaction_failed);
        errorDialog.setMessage(throwable.getMessage());
        errorDialog.show();
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        switch (errorEnvelope.code)
        {
            case C.ErrorCode.INSUFFICIENT_BALANCE:
                sourceSelector.setError(getString(R.string.error_insufficient_balance, sourceSelector.getToken().symbol));
                break;
            case C.ErrorCode.SWAP_TIMEOUT_ERROR:
                startQuoteTask();
                break;
            case C.ErrorCode.SWAP_CONNECTIONS_ERROR:
            case C.ErrorCode.SWAP_CHAIN_ERROR:
                errorDialog = new AWalletAlertDialog(this);
                errorDialog.setTitle(R.string.title_dialog_error);
                errorDialog.setMessage(errorEnvelope.message);
                errorDialog.setButton(R.string.try_again, v -> {
                    viewModel.getChains();
                    errorDialog.dismiss();
                });
                errorDialog.setSecondaryButton(R.string.action_cancel, v -> errorDialog.dismiss());
                errorDialog.show();
                break;
            case C.ErrorCode.SWAP_QUOTE_ERROR:
                errorDialog = new AWalletAlertDialog(this);
                errorDialog.setTitle(R.string.title_dialog_error);
                errorDialog.setMessage(errorEnvelope.message);
                errorDialog.setButton(R.string.try_again, v -> {
                    startQuoteTask();
                    errorDialog.dismiss();
                });
                errorDialog.setSecondaryButton(R.string.action_cancel, v -> errorDialog.dismiss());
                errorDialog.show();
                break;
            default:
                errorDialog = new AWalletAlertDialog(this);
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

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthentication(this, wallet, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction tx)
    {
        viewModel.sendTransaction(tx, wallet, settingsDialog.getSelectedChainId());
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {

    }

    @Override
    public void notifyConfirm(String mode)
    {

    }

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return null;
    }
}
