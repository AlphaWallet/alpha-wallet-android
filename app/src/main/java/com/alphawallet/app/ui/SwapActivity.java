package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.analytics.ActionSheetSource;
import com.alphawallet.app.entity.lifi.Chain;
import com.alphawallet.app.entity.lifi.Connection;
import com.alphawallet.app.entity.lifi.Quote;
import com.alphawallet.app.entity.lifi.Token;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.ProgressInfo;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.SwapUtils;
import com.alphawallet.app.viewmodel.SwapViewModel;
import com.alphawallet.app.viewmodel.Tokens;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.SelectTokenDialog;
import com.alphawallet.app.widget.StandardHeader;
import com.alphawallet.app.widget.SwapSettingsDialog;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.app.widget.TokenSelector;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.tools.Numeric;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SwapActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
    private static final long GET_QUOTE_INTERVAL_MS = 30000;
    private static final long COUNTDOWN_INTERVAL_MS = 1000;
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
    private StandardHeader quoteHeader;
    private TokenInfoView provider;
    private TokenInfoView providerWebsite;
    private TokenInfoView gasFees;
    private TokenInfoView otherFees;
    private TokenInfoView currentPrice;
    private TokenInfoView minReceived;
    private LinearLayout noConnectionsLayout;
    private MaterialButton continueBtn;
    private MaterialButton openSettingsBtn;
    private TextView chainName;
    private com.alphawallet.app.entity.tokens.Token token;
    private Wallet wallet;
    private Token sourceToken;
    private List<Chain> chains;
    private String selectedRouteProvider;
    private CountDownTimer getQuoteTimer;
    private ActivityResultLauncher<Intent> selectSwapProviderLauncher;
    private ActivityResultLauncher<Intent> gasSettingsLauncher;
    private ActivityResultLauncher<Intent> getRoutesLauncher;
    private AnalyticsProperties confirmationDialogProps;

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

        initTimer();

        registerActivityResultLaunchers();

        viewModel.prepare(this, selectSwapProviderLauncher);
    }

    private void registerActivityResultLaunchers()
    {
        selectSwapProviderLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK)
                    {
                        viewModel.getChains();
                    }
                });

        gasSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> confirmationDialog.setCurrentGasIndex(result));

        getRoutesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK)
                    {
                        Intent data = result.getData();
                        if (data != null)
                        {
                            selectedRouteProvider = data.getStringExtra("provider");
                            getQuote();
                        }
                    }
                    else if (result.getResultCode() == RESULT_CANCELED)
                    {
                        continueBtn.setEnabled(!TextUtils.isEmpty(selectedRouteProvider));
                    }
                });
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

    private void initTimer()
    {
        getQuoteTimer = new CountDownTimer(GET_QUOTE_INTERVAL_MS, COUNTDOWN_INTERVAL_MS)
        {
            @Override
            public void onTick(long millisUntilFinished)
            {
                // TODO: Display countdown timer?
            }

            @Override
            public void onFinish()
            {
                getQuote();
            }
        };
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
        quoteHeader = findViewById(R.id.header_quote);
        provider = findViewById(R.id.tiv_provider);
        providerWebsite = findViewById(R.id.tiv_provider_website);
        gasFees = findViewById(R.id.tiv_gas_fees);
        otherFees = findViewById(R.id.tiv_other_fees);
        currentPrice = findViewById(R.id.tiv_current_price);
        minReceived = findViewById(R.id.tiv_min_received);
        noConnectionsLayout = findViewById(R.id.layout_no_connections);
        continueBtn = findViewById(R.id.btn_continue);
        openSettingsBtn = findViewById(R.id.btn_open_settings);

        quoteHeader.getImageControl().setOnClickListener(v -> {
            getAvailableRoutes();
        });

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
                if (TextUtils.isEmpty(selectedRouteProvider))
                {
                    getAvailableRoutes();
                }
                else
                {
                    getQuote();
                }
            }

            @Override
            public void onSelectionChanged(Token token)
            {
                sourceTokenChanged(token);
            }

            @Override
            public void onMaxClicked()
            {
                Token token = sourceSelector.getToken();
                if (token == null)
                {
                    return;
                }

                String max = viewModel.getBalance(token);
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
            public void onSelectionChanged(Token token)
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
            viewModel.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION, confirmationDialogProps);
        }
    }

    private ActionSheetDialog createConfirmationAction(Quote quote)
    {
        ActionSheetDialog confDialog = null;
        try
        {
            com.alphawallet.app.entity.tokens.Token activeToken = viewModel.getTokensService().getTokenOrBase(sourceToken.chainId, sourceToken.address);
            Web3Transaction w3Tx = viewModel.buildWeb3Transaction(quote);
            confDialog = new ActionSheetDialog(this, w3Tx, activeToken,
                    "", w3Tx.recipient.toString(), viewModel.getTokensService(), this);
            confDialog.setURL(quote.swapProvider.name);
            confDialog.setCanceledOnTouchOutside(false);
            confDialog.setGasEstimate(Numeric.toBigInt(quote.transactionRequest.gasLimit));

            confirmationDialogProps = new AnalyticsProperties();
            confirmationDialogProps.put(Analytics.PROPS_ACTION_SHEET_SOURCE, ActionSheetSource.SWAP.getValue());
            confirmationDialogProps.put(Analytics.PROPS_SWAP_FROM_TOKEN, quote.action.fromToken.symbol);
            confirmationDialogProps.put(Analytics.PROPS_SWAP_TO_TOKEN, quote.action.toToken.symbol);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return confDialog;
    }

    private void destTokenChanged(Token token)
    {
        destSelector.setBalance(viewModel.getBalance(token));

        infoLayout.setVisibility(View.GONE);

        destTokenDialog.setSelectedToken(token.address);

        selectedRouteProvider = "";

        getAvailableRoutes();
    }

    private void sourceTokenChanged(Token token)
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

        selectedRouteProvider = "";

        getAvailableRoutes();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.track(Analytics.Navigation.TOKEN_SWAP);

        if (settingsDialog != null)
        {
            settingsDialog.setSwapProviders(viewModel.getPreferredSwapProviders());
        }
    }

    @Override
    protected void onPause()
    {
        if (getQuoteTimer != null)
        {
            getQuoteTimer.cancel();
        }
        super.onPause();
    }

    // The source token should default to the token selected in the main wallet dialog (ie the token from the intent).
    private void initSourceToken(Token selectedToken)
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

    private void initFromDialog(List<Token> fromTokens)
    {
        Tokens.sortValue(fromTokens);
        sourceTokenDialog = new SelectTokenDialog(fromTokens, this, tokenItem -> {
            sourceSelector.init(tokenItem);
            sourceTokenDialog.dismiss();
        });
    }

    private void initToDialog(List<Token> toTokens)
    {
        Tokens.sortName(toTokens);
        Tokens.sortValue(toTokens);
        destTokenDialog = new SelectTokenDialog(toTokens, this, tokenItem -> {
            destSelector.init(tokenItem);
            destTokenDialog.dismiss();
        });
    }

    private void getAvailableRoutes()
    {
        if (getQuoteTimer != null)
        {
            getQuoteTimer.cancel();
        }

        if (sourceSelector.getToken() != null
                && destSelector.getToken() != null
                && !sourceSelector.getToken().equals(destSelector.getToken())
                && !TextUtils.isEmpty(sourceSelector.getAmount()))
        {
            viewModel.getRoutes(
                    this,
                    getRoutesLauncher,
                    sourceSelector.getToken(),
                    destSelector.getToken(),
                    wallet.address,
                    sourceSelector.getAmount(),
                    settingsDialog.getSlippage()
            );
        }
    }

    private void onChains(List<Chain> chains)
    {
        this.chains = chains;

        settingsDialog = new SwapSettingsDialog(
                this,
                chains,
                viewModel.getSwapProviders(),
                viewModel.getPreferredSwapProviders(),
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
            List<Token> fromTokens = new ArrayList<>();
            List<Token> toTokens = new ArrayList<>();
            Token selectedToken = null;

            for (Connection c : connections)
            {
                for (Token t : c.fromTokens)
                {
                    if (!fromTokens.contains(t))
                    {
                        t.balance = viewModel.getBalance(t);
                        t.fiatEquivalent = t.getFiatValue();

                        if (t.fiatEquivalent > 0)
                        {
                            fromTokens.add(t);

                            if (t.isSimilarTo(token, wallet.address))
                            {
                                selectedToken = t;
                            }
                        }
                    }
                }

                for (Token t : c.toTokens)
                {
                    if (!toTokens.contains(t))
                    {
                        t.balance = viewModel.getBalance(t);
                        t.fiatEquivalent = t.getFiatValue();
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

    private void getQuote()
    {
        if (!TextUtils.isEmpty(selectedRouteProvider))
        {
            if (errorDialog != null && errorDialog.isShowing())
            {
                errorDialog.dismiss();
            }

            viewModel.getQuote(
                    sourceSelector.getToken(),
                    destSelector.getToken(),
                    wallet.address,
                    sourceSelector.getAmount(),
                    settingsDialog.getSlippage(),
                    selectedRouteProvider
            );
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

        getQuoteTimer.start();
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
        provider.setValue(quote.swapProvider.name);
        String url = viewModel.getSwapProviderUrl(quote.swapProvider.key);
        if (!TextUtils.isEmpty(url))
        {
            providerWebsite.setValue(url);
            providerWebsite.setLink();
        }
        gasFees.setValue(SwapUtils.getTotalGasFees(quote.estimate.gasCosts));
        otherFees.setValue(SwapUtils.getOtherFees(quote.estimate.feeCosts));
        currentPrice.setValue(SwapUtils.getFormattedCurrentPrice(quote.action).trim());
        minReceived.setValue(SwapUtils.getFormattedMinAmount(quote.estimate, quote.action));
        infoLayout.setVisibility(View.VISIBLE);
    }

    private void onProgressInfo(ProgressInfo progressInfo)
    {
        if (progressInfo.shouldShow())
        {
            progressDialog.setTitle(progressInfo.getMessage());
            progressDialog.show();
        }
        else
        {
            progressDialog.dismiss();
        }
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

        viewModel.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_SUCCESSFUL, confirmationDialogProps);
    }

    private void txError(Throwable throwable)
    {
        AWalletAlertDialog errorDialog = new AWalletAlertDialog(this);
        errorDialog.setTitle(R.string.error_transaction_failed);
        errorDialog.setMessage(throwable.getMessage());
        errorDialog.show();

        confirmationDialogProps.put(Analytics.PROPS_ERROR_MESSAGE, throwable.getMessage());
        viewModel.track(Analytics.Navigation.ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_FAILED, confirmationDialogProps);
    }

    private void onError(ErrorEnvelope errorEnvelope)
    {
        switch (errorEnvelope.code)
        {
            case C.ErrorCode.INSUFFICIENT_BALANCE:
                sourceSelector.setError(getString(R.string.error_insufficient_balance, sourceSelector.getToken().symbol));
                break;
            case C.ErrorCode.SWAP_TIMEOUT_ERROR:
                getAvailableRoutes();
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
                viewModel.trackError(Analytics.Error.TOKEN_SWAP, errorEnvelope.message);
                break;
            case C.ErrorCode.SWAP_QUOTE_ERROR:
                errorDialog = new AWalletAlertDialog(this);
                errorDialog.setTitle(R.string.title_dialog_error);
                errorDialog.setMessage(errorEnvelope.message);
                errorDialog.setButton(R.string.try_again, v -> {
                    getAvailableRoutes();
                    errorDialog.dismiss();
                });
                errorDialog.setSecondaryButton(R.string.action_cancel, v -> errorDialog.dismiss());
                errorDialog.show();
                viewModel.trackError(Analytics.Error.TOKEN_SWAP, errorEnvelope.message);
                break;
            default:
                errorDialog = new AWalletAlertDialog(this);
                errorDialog.setTitle(R.string.title_dialog_error);
                errorDialog.setMessage(errorEnvelope.message);
                errorDialog.setButton(R.string.action_cancel, v -> errorDialog.dismiss());
                errorDialog.show();
                viewModel.trackError(Analytics.Error.TOKEN_SWAP, errorEnvelope.message);
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
        if (wallet.type != WalletType.WATCH)
        {
            viewModel.getAuthentication(this, wallet, callback);
        }
        else
        {
            confirmationDialog.dismiss();
            errorDialog = new AWalletAlertDialog(this);
            errorDialog.setTitle(R.string.title_dialog_error);
            errorDialog.setMessage(getString(R.string.error_message_watch_only_wallet));
            errorDialog.setButton(R.string.dialog_ok, v -> errorDialog.dismiss());
            errorDialog.show();
        }
    }

    @Override
    public void sendTransaction(Web3Transaction tx)
    {
        viewModel.sendTransaction(tx, wallet, settingsDialog.getSelectedChainId());
    }

    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        if (!actionCompleted && TextUtils.isEmpty(txHash))
        {
            viewModel.track(Analytics.Action.ACTION_SHEET_CANCELLED, confirmationDialogProps);
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {
        confirmationDialogProps.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        viewModel.track(Analytics.Action.ACTION_SHEET_COMPLETED, confirmationDialogProps);
    }

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return gasSettingsLauncher;
    }
}
