package com.alphawallet.app.ui;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.FinishReceiver;
import com.alphawallet.app.entity.GasSettings;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BackupKeyViewModel;
import com.alphawallet.app.viewmodel.ConfirmationViewModel;
import com.alphawallet.app.viewmodel.ConfirmationViewModelFactory;
import com.alphawallet.app.viewmodel.GasSettingsViewModel;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.alphawallet.token.tools.Numeric;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.C.ETH_SYMBOL;
import static com.alphawallet.app.C.GAS_LIMIT_MIN;
import static com.alphawallet.app.C.PRUNE_ACTIVITY;
import static com.alphawallet.app.C.RESET_WALLET;
import static com.alphawallet.app.entity.ConfirmationType.ETH;
import static com.alphawallet.app.entity.ConfirmationType.WEB3TRANSACTION;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static com.alphawallet.token.tools.Convert.getEthString;

public class ConfirmationActivity extends BaseActivity implements SignAuthenticationCallback
{
    private static final String NONCE_LOW_MESSAGE = "too low";
    private static final String NONCE_STRING = "nonce";
    AWalletAlertDialog dialog;

    @Inject
    ConfirmationViewModelFactory confirmationViewModelFactory;
    ConfirmationViewModel viewModel;

    private FinishReceiver finishReceiver;

    private TextView fromAddressText;
    private TextView toAddressText;
    private TextView valueText;
    private TextView symbolText;
    private TextView gasPriceText;
    private TextView gasLimitText;
    private TextView networkFeeText;
    private TextView contractAddrText;
    private TextView contractAddrLabel;
    private TextView websiteLabel;
    private TextView websiteText;
    private Button sendButton;
    private Button moreDetail;
    private TextView title;
    private TextView chainName;
    private TextView gasEstimateText;
    private ProgressBar progressGasEstimate;
    private ProgressBar progressNetworkFee;
    private GasSettings localGasSettings;
    private ImageView triangleImage;
    private TextView infoText;
    private View parentLayout;
    private View valueLayout;

    private BigDecimal amount;
    private String tokenIds;
    private BigInteger gasPrice;
    private int decimals;
    private String contractAddress;
    private String to;
    private String transactionHex;
    private Token token;
    private int chainId;
    private Wallet sendingWallet;
    private BigInteger nonce;
    private String oldTxHash = null;
    private BigInteger oldGasPrice = BigInteger.ZERO;
    private BigInteger oldGasLimit = BigInteger.ZERO;
    private String transactionAddress; // The actual transaction address - used for estimating gas

    private ConfirmationType confirmationType;
    private byte[] transactionBytes = null;
    private Web3Transaction transaction;
    private boolean sendingAllFunds = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_confirm);
        transaction = null;
        toolbar();

        setTitle(getString(R.string.title_transaction_details));
        fromAddressText = findViewById(R.id.text_from);
        toAddressText = findViewById(R.id.text_to);
        valueText = findViewById(R.id.text_value);
        symbolText = findViewById(R.id.text_symbol);
        gasPriceText = findViewById(R.id.text_gas_price);
        gasLimitText = findViewById(R.id.text_gas_limit);
        networkFeeText = findViewById(R.id.text_network_fee);
        sendButton = findViewById(R.id.send_button);
        moreDetail = findViewById(R.id.more_detail);
        contractAddrText = findViewById(R.id.text_contract);
        contractAddrLabel = findViewById(R.id.label_contract);
        websiteLabel = findViewById(R.id.label_website);
        websiteText = findViewById(R.id.text_website);
        title = findViewById(R.id.title_confirm);
        chainName = findViewById(R.id.text_chain_name);
        gasEstimateText = findViewById(R.id.text_gas_estimate);
        progressGasEstimate = findViewById(R.id.progress_gas_estimate);
        progressNetworkFee = findViewById(R.id.progress_network_fee);
        triangleImage = findViewById(R.id.image_triangle);
        infoText = findViewById(R.id.text_info);
        valueLayout = findViewById(R.id.layout_value);
        parentLayout = findViewById(R.id.layout_parent);
        sendButton.setOnClickListener(view -> onSend());

        transaction = getIntent().getParcelableExtra(C.EXTRA_WEB3TRANSACTION);

        to = getIntent().getStringExtra(C.EXTRA_TO_ADDRESS);
        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        confirmationType = ConfirmationType.values()[getIntent().getIntExtra(C.TOKEN_TYPE, 0)];
        String ensName = getIntent().getStringExtra(C.EXTRA_ENS_DETAILS);
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, -1);
        String symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        tokenIds = getIntent().getStringExtra(C.EXTRA_TOKENID_LIST);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        chainId = token != null ? token.tokenInfo.chainId : getIntent().getIntExtra(C.EXTRA_NETWORKID, 1);
        String functionDetails = getIntent().getStringExtra(C.EXTRA_FUNCTION_NAME);
        String gasPriceStr = getIntent().getStringExtra(C.EXTRA_GAS_PRICE);
        gasPrice = TextUtils.isEmpty(gasPriceStr) ? BigInteger.ZERO : new BigInteger(gasPriceStr);

        if (Utils.isAddressValid(contractAddress))
        {
            transactionAddress = contractAddress;
        }
        else
        {
            transactionAddress = to; // in the case of native transactions
        }

        String amountString;
        int nonceId;

        viewModel = new ViewModelProvider(this, confirmationViewModelFactory)
                .get(ConfirmationViewModel.class);

        setupViewListeners();
        symbol = symbol == null ? viewModel.getNetworkSymbol(chainId) : symbol;

        Utils.setChainColour(chainName, chainId);
        chainName.setText(viewModel.getNetworkName(chainId));

        try
        {
            amount = new BigDecimal(getIntent().getStringExtra(C.EXTRA_AMOUNT));
        }
        catch (NumberFormatException e)
        {
            amount = BigDecimal.ZERO;
        }

        if (token == null)
            token = viewModel.getToken(chainId, contractAddress);

        switch (confirmationType) {
            case ETH:
                amountString = "-" + BalanceUtils.getScaledValueWithLimit(amount, decimals);
                if (gasPrice.compareTo(BigInteger.ZERO) > 0)
                {
                    amountString += getString(R.string.bracketed, getString(R.string.all_funds));
                    sendingAllFunds = true;
                }
                symbolText.setText(symbol);
                transactionBytes = null;
                break;
            case ERC20:
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = "-" + BalanceUtils.getScaledValueWithLimit(amount, decimals);
                symbolText.setText(symbol);
                transactionBytes = TokenRepository.createTokenTransferData(to, amount.toBigInteger());
                break;
            case ERC875:
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = tokenIds;
                transactionBytes = viewModel.getERC875TransferBytes(to, contractAddress, tokenIds, chainId);
                break;
            case MARKET:
                amountString = tokenIds;
                to = "Stormbird market";
                break;
            case TOKENSCRIPT:
                title.setVisibility(View.VISIBLE);
                title.setText(R.string.confirm_tokenscript_transaction);
                to = getIntent().getStringExtra(C.EXTRA_ACTION_NAME);
                ((TextView)findViewById(R.id.title_to)).setText(R.string.tokenscript_call);

                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = getIntent().getStringExtra(C.EXTRA_ACTION_NAME);
                if (!TextUtils.isEmpty(functionDetails)) amountString = functionDetails;
                symbolText.setVisibility(View.GONE);

                transactionHex = getIntent().getStringExtra(C.EXTRA_TRANSACTION_DATA);
                if (transactionHex != null) transactionBytes = Numeric.hexStringToByteArray(transactionHex);

                break;
            case WEB3TRANSACTION:
                title.setVisibility(View.VISIBLE);
                title.setText(R.string.confirm_dapp_transaction);
                to = transaction.recipient.toString();
                transactionAddress = to;
                if (transaction.payload == null) //pure ETH transaction
                {
                    confirmationType = ETH;
                    transactionBytes = null;
                }
                else
                {
                    BigInteger addr = Numeric.toBigInt(transaction.recipient.toString());
                    if (addr.equals(BigInteger.ZERO)) //constructor
                    {
                        to = getString(R.string.ticket_contract_constructor);
                    }
                    else //function call to contract
                    {
                        moreDetail.setVisibility(View.VISIBLE);
                        moreDetail.setOnClickListener(v -> { viewModel.showMoreDetails(this, to, chainId); }); // allow user to check out contract
                    }
                    transactionBytes = Numeric.hexStringToByteArray(transaction.payload);
                }
                String urlRequester = getIntent().getStringExtra(C.EXTRA_ACTION_NAME);
                checkTransactionGas();

                if (urlRequester != null)
                {
                    websiteLabel.setVisibility(View.VISIBLE);
                    websiteText.setVisibility(View.VISIBLE);
                    websiteText.setText(urlRequester);
                }

                BigDecimal ethAmount = Convert.fromWei(transaction.value.toString(10), Convert.Unit.ETHER);
                amountString = getEthString(ethAmount.doubleValue());
                symbolText.setText(ETH_SYMBOL);
                break;
            case ERC721:
                String contractName = getIntent().getStringExtra(C.EXTRA_ACTION_NAME);
                title.setVisibility(View.VISIBLE);
                title.setText(R.string.confirm_erc721_transfer);
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                String contractTxt = contractAddress + " " + contractName;
                contractAddrText.setText(contractTxt);
                symbolText.setText(token.getSymbol());
                amountString = symbol;
                transactionBytes = viewModel.getERC721TransferBytes(to, contractAddress, tokenIds, chainId);
                break;
            case RESEND:
                setTitle(getString(R.string.speedup_transaction));
                oldTxHash = getIntent().getStringExtra(C.EXTRA_TXHASH);
                nonceId = getIntent().getIntExtra(C.EXTRA_NONCE, 0);
                nonce = BigInteger.valueOf(nonceId);
                amountString = getString(R.string.speedup_tx_description);
                showTooltip(amountString, v -> {
                    viewModel.openGasSettings(this, chainId); //if tooltip is clicked on open gas settings
                });
                oldGasPrice = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
                oldGasLimit = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));
                transactionHex = getIntent().getStringExtra(C.EXTRA_TRANSACTION_DATA);
                if (!TextUtils.isEmpty(transactionHex)) transactionBytes = Numeric.hexStringToByteArray(transactionHex);
                setupResendGasSettings();
                break;
            case CANCEL_TX:
                setTitle(getString(R.string.cancel_transaction));
                oldTxHash = getIntent().getStringExtra(C.EXTRA_TXHASH);
                nonceId = getIntent().getIntExtra(C.EXTRA_NONCE, 0);
                nonce = BigInteger.valueOf(nonceId);
                amount = BigDecimal.ZERO;
                amountString = getString(R.string.cancel_tx_description);
                valueText.setTextColor(getColor(R.color.text_black));
                symbolText.setVisibility(View.GONE);
                oldGasPrice = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_PRICE));
                oldGasLimit = new BigInteger(getIntent().getStringExtra(C.EXTRA_GAS_LIMIT));
                transactionHex = "0x";
                setupCancelGasSettings();
                break;
            default:
                amountString = "-" + BalanceUtils.getScaledValueWithLimit(amount, decimals);
                symbolText.setText(symbol);
                transactionBytes = null;
                break;
        }

        if (ensName != null && ensName.length() > 0)
        {
            toAddressText.setText(ensName);
        }
        else
        {
            toAddressText.setText(to);
        }

        valueText.setText(amountString);

        findViewById(R.id.layout_gas_price).setOnClickListener(view -> {
            //open gas slider settings
            viewModel.openGasSettings(this, chainId);
        });

        findViewById(R.id.layout_gas_limit).setOnClickListener(view -> {
            //open gas slider settings
            viewModel.openGasSettingsLimitOpen(this, chainId);
        });

        gasLimitText.setOnClickListener(view -> {
            //open gas slider settings
            viewModel.openGasSettingsLimitOpen(this, chainId);
        });

        getGasSettings();
    }

    private void showTooltip(String tooltipInfo, View.OnClickListener listener)
    {
        //TODO: Can use a RelativeLayout overlay in the view and let the OS do all work, then you only need a couple of lines of code here
        infoText.setVisibility(View.VISIBLE);
        triangleImage.setVisibility(View.VISIBLE);
        infoText.setText(tooltipInfo);
        symbolText.setVisibility(View.GONE);
        chainName.setVisibility(View.GONE);
        valueText.setVisibility(View.GONE);
        int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.dp25);
        parentLayout.setPadding(parentLayout.getPaddingLeft(), 0, parentLayout.getPaddingRight(), parentLayout.getPaddingBottom());
        valueLayout.setPadding(valueLayout.getPaddingLeft(), 0, valueLayout.getPaddingRight(), paddingBottom);

        infoText.setOnClickListener(listener);
    }

    private void setupViewListeners()
    {
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.gasSettings().observe(this, this::onGasSettings);
        viewModel.sendTransaction().observe(this, this::onTransaction);
        viewModel.sendDappTransaction().observe(this, this::onDappTransaction);
        viewModel.progress().observe(this, this::onProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.pushToast().observe(this, this::displayToast);
        viewModel.sendGasSettings().observe(this, this::onSendGasSettings);
        viewModel.sendGasEstimate().observe(this, this::onGasEstimate);
        viewModel.sendGasEstimateError().observe(this, this::onEstimateError);
        finishReceiver = new FinishReceiver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!EthereumNetworkRepository.hasGasOverride(chainId))
        {
            getMenuInflater().inflate(R.menu.menu_settings, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                viewModel.openGasSettings(this, chainId);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.resetSignDialog();
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress)
        {
            dialog = new AWalletAlertDialog(this);
            dialog.setProgressMode();
            dialog.setTitle(R.string.title_dialog_sending);
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideDialog();
        unregisterReceiver(finishReceiver);
        viewModel.stopGasUpdate();
    }

    private void onSend()
    {
        viewModel.getGasForSending(confirmationType, this, chainId);
    }

    private void onSendGasSettings(GasSettings gasSettings)
    {
        localGasSettings = gasSettings;
        if (gasSettings != null) viewModel.getAuthorisation(this, this);
    }

    private void finaliseTransaction()
    {
        switch (confirmationType)
        {
            case ETH:
                viewModel.createTransaction(
                        sendingWallet,
                        to,
                        amount.toBigInteger(),
                        localGasSettings.gasPrice,
                        localGasSettings.gasLimit,
                        chainId);
                break;

            case ERC20:
                viewModel.createTokenTransfer(
                        sendingWallet,
                        to,
                        contractAddress,
                        amount.toBigInteger(),
                        localGasSettings.gasPrice,
                        localGasSettings.gasLimit,
                        chainId);
                break;

            case ERC875:
                viewModel.createTicketTransfer(
                        sendingWallet,
                        to,
                        contractAddress,
                        tokenIds,
                        localGasSettings.gasPrice,
                        localGasSettings.gasLimit,
                        chainId);
                break;

            case WEB3TRANSACTION:
                viewModel.signWeb3DAppTransaction(transaction, localGasSettings.gasPrice, localGasSettings.gasLimit, chainId);
                break;

            case TOKENSCRIPT:
                viewModel.signTokenScriptTransaction(transactionHex, contractAddress, localGasSettings.gasPrice, localGasSettings.gasLimit, amount.toBigInteger(), chainId);
                break;

            case ERC721:
                viewModel.createERC721Transfer(
                        to,
                        contractAddress,
                        tokenIds,
                        localGasSettings.gasPrice,
                        localGasSettings.gasLimit,
                        chainId);
                break;

            case RESEND:
            case CANCEL_TX:
                viewModel.sendOverrideTransaction(
                        transactionHex, to, nonce, localGasSettings.gasPrice, localGasSettings.gasLimit, amount.toBigInteger(), chainId);
                break;

            default:
                break;
        }
    }

    private void onDefaultWallet(Wallet wallet) {
        fromAddressText.setText(wallet.address);
        sendingWallet = wallet;

        progressGasEstimate.setVisibility(View.VISIBLE);
        progressNetworkFee.setVisibility(View.VISIBLE);
        viewModel.calculateGasEstimate(transactionBytes, chainId, transactionAddress, amount.toBigInteger());
    }

    private void onTransaction(String hash) {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setCancelable(true);
        dialog.setMessage(hash);
        dialog.setButtonText(R.string.ok);
        dialog.setSecondaryButtonText(R.string.copy);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.setSecondaryButtonListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction hash",
                    EthereumNetworkBase.getEtherscanURLbyNetwork(token.tokenInfo.chainId) + "tx/" + hash);
            clipboard.setPrimaryClip(clip);
            dialog.dismiss();
            sendBroadcast(new Intent(PRUNE_ACTIVITY));
        });
        dialog.setOnDismissListener(v -> {
            dialog.dismiss();
            new HomeRouter().open(this, true);
            finish();
        });
        dialog.show();

        if (oldTxHash != null)
        {
            viewModel.removeOverridenTransaction(oldTxHash);
            sendBroadcast(new Intent(RESET_WALLET)); //refresh transactions list
        }
    }

    private void onDappTransaction(TransactionData txData) {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setMessage(txData.txHash);
        dialog.setButtonText(R.string.copy);
        dialog.setButtonListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction hash",
                    EthereumNetworkBase.getEtherscanURLbyNetwork(chainId) + "tx/" + txData.txHash);
            clipboard.setPrimaryClip(clip);
            dialog.dismiss();
        });
        dialog.setOnDismissListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_WEB3TRANSACTION, transaction);
            intent.putExtra(C.EXTRA_HEXDATA, txData.signature);
            intent.putExtra(C.EXTRA_TRANSACTION_DATA, txData.txHash);
            setResult(RESULT_OK, intent);
            finish();
        });
        dialog.show();
    }

    private void checkTransactionGas()
    {
        BigInteger limit = BigInteger.ZERO;
        BigInteger price = BigInteger.ZERO;
        if (transaction.gasLimit != null && transaction.gasLimit.compareTo(BigInteger.ZERO) > 0) limit = transaction.gasLimit;
        if (transaction.gasPrice != null && transaction.gasPrice.compareTo(BigInteger.ZERO) > 0) price = transaction.gasPrice;

        if (!price.equals(BigInteger.ZERO) || !limit.equals(BigInteger.ZERO))
        {
            GasSettings override = new GasSettings(price, limit);
            viewModel.overrideGasSettings(override);
            onGasSettings(override);
        }
    }

    private void getGasSettings()
    {
        viewModel.startGasUpdate(chainId);
        switch (confirmationType)
        {
            case ERC875:
            case ERC721:
                viewModel.calculateGasSettings(transactionBytes, true, chainId);
                break;
            default:
                if (!viewModel.hasGasOverride())
                {
                    viewModel.calculateGasSettings(transactionBytes, false, chainId);
                }
                break;
        }
    }

    private void setupCancelGasSettings()
    {
        BigInteger gasPrice = oldGasPrice.add(BalanceUtils.gweiToWei(BigDecimal.valueOf(2))); //doesn't need more than this for cancel

        findViewById(R.id.layout_old_gas_price).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.old_gas_price)).setText(BalanceUtils.weiToGwei(oldGasPrice));

        String gasPriceStr = BalanceUtils.weiToGwei(gasPrice);

        gasPriceText.setText(gasPriceStr);
        GasSettings overrideSettings = new GasSettings(gasPrice, oldGasLimit);
        viewModel.overrideGasSettings(overrideSettings);
    }

    private void setupResendGasSettings()
    {
        //increase gas price - gas price is in GWEI, so add 2 GWEI to price
        BigInteger gasPrice = oldGasPrice.add(BalanceUtils.gweiToWei(BigDecimal.valueOf(2)));

        findViewById(R.id.layout_old_gas_price).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.old_gas_price)).setText(BalanceUtils.weiToGwei(oldGasPrice));

        String gasPriceStr = BalanceUtils.weiToGwei(gasPrice);

        gasPriceText.setText(gasPriceStr);
        GasSettings overrideSettings = new GasSettings(gasPrice, oldGasLimit);
        viewModel.overrideGasSettings(overrideSettings);
    }

    private void onGasSettings(GasSettings gasSettings) {
        String gasPriceStr = BalanceUtils.weiToGwei(gasSettings.gasPrice);
        gasPrice = gasSettings.gasPrice;
        gasPriceText.setText(gasPriceStr);
        gasLimitText.setText(gasSettings.gasLimit.toString());

        BigDecimal networkFeeBD = new BigDecimal(gasSettings.gasPrice.multiply(gasSettings.gasLimit));

        // Add the network fee to the expected tx price for Dapp sourced transactions
        // Should we add the network fee for all transactions?
        switch (confirmationType)
        {
            case ETH:
                if (sendingAllFunds)
                {
                    String amountString;
                    if (gasSettings.gasLimit.compareTo(BigInteger.valueOf(GAS_LIMIT_MIN)) != 0) //did user adjust gas limit?
                    {
                        sendingAllFunds = false;
                        amountString = "-" + BalanceUtils.getScaledValueWithLimit(amount, decimals);
                    }
                    else
                    {
                        //adjust value as appropriate
                        amount = token.balance.subtract(networkFeeBD);
                        //patch new value
                        amountString = "-" + BalanceUtils.getScaledValueWithLimit(amount, decimals) + getString(R.string.bracketed, getString(R.string.all_funds));
                    }
                    valueText.setText(amountString);
                }
                break;
            case WEB3TRANSACTION:
                BigDecimal ethValueBD = amount.add(networkFeeBD);
                //convert to ETH
                ethValueBD = Convert.fromWei(ethValueBD, Convert.Unit.ETHER);
                String valueUpdate = getEthString(ethValueBD.doubleValue());
                valueText.setText(valueUpdate);
                break;
            default:
                break;
        }
    }

    private void onGasEstimate(BigInteger gasEstimate) {
        progressGasEstimate.setVisibility(View.GONE);
        progressNetworkFee.setVisibility(View.GONE);

        BigDecimal networkFeeBD = new BigDecimal(gasPrice.multiply(gasEstimate));

        String networkFee = BalanceUtils.getScaledValue(networkFeeBD, C.ETHER_DECIMALS, 8)
                + " " + viewModel.getNetworkSymbol(chainId);
        networkFeeText.setText(networkFee);
        gasEstimateText.setText(String.valueOf(gasEstimate));

        viewModel.updateGasLimit();
    }

    private void onEstimateError(ErrorEnvelope error) {
        progressGasEstimate.setVisibility(View.GONE);
        progressNetworkFee.setVisibility(View.GONE);

        //for now, revert to old behaviour; do more testing; design UX for this
        findViewById(R.id.layout_gas_estimate).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.text_network_fee_title)).setText(R.string.label_network_fee);
        BigDecimal networkFeeBD = new BigDecimal(gasPrice.multiply(viewModel.gasSettings().getValue().gasLimit));
        String networkFee = BalanceUtils.getScaledValue(networkFeeBD, C.ETHER_DECIMALS, 8)
                + " " + viewModel.getNetworkSymbol(chainId);
        networkFeeText.setText(networkFee);
    }

    private void onError(ErrorEnvelope error) {
        hideDialog();
        dialog = new AWalletAlertDialog(this);

        String errorMessage = error.message != null ? error.message.toLowerCase() : "";
        if (confirmationType == ConfirmationType.RESEND || confirmationType == ConfirmationType.CANCEL_TX && errorMessage.contains(NONCE_LOW_MESSAGE) && errorMessage.contains(NONCE_STRING))
        {
            dialog.setIcon(WARNING);
            dialog.setTitle(R.string.transaction_already_written_title);
            dialog.setMessage(R.string.transaction_already_written);
        }
        else
        {
            dialog.setIcon(ERROR);
            dialog.setTitle(R.string.error_transaction_failed);
            dialog.setMessage(error.message);
        }

        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
            if (confirmationType == WEB3TRANSACTION)
            {
                Intent intent = new Intent();
                intent.putExtra(C.EXTRA_WEB3TRANSACTION, transaction);
                intent.putExtra(C.EXTRA_HEXDATA, "0x0000"); //Placeholder signature - transaction failed
                intent.putExtra(C.EXTRA_SUCCESS, false);
                setResult(RESULT_CANCELED, intent);
            }
            if (error.message == null || !error.message.equals(getString(R.string.authentication_error))) finish();
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode,resultCode,intent);

        if (requestCode == C.SET_GAS_SETTINGS)
        {
            if (resultCode == RESULT_OK)
            {
                BigInteger gasPrice = new BigInteger(intent.getStringExtra(C.EXTRA_GAS_PRICE));
                BigInteger gasLimit = new BigInteger(intent.getStringExtra(C.EXTRA_GAS_LIMIT));
                GasSettings settings = new GasSettings(gasPrice, gasLimit);
                viewModel.overrideGasSettings(settings);
            }
        }
        else if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            gotAuthorisation(resultCode == RESULT_OK);
        }
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (gotAuth)
        {
            onProgress(true);
            Completable.fromAction(this::finaliseTransaction) //sign on a computation thread to give UI a chance to complete all tasks
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()
                    .isDisposed();
        }
        else
        {
            //fail authentication
            securityError();
        }
    }

    @Override
    public void cancelAuthentication()
    {
        hideDialog();
    }

    private void securityError() {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.key_error);
        dialog.setMessage(getString(R.string.error_while_signing_transaction));
        dialog.setButtonText(R.string.ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        if (getApplicationContext() != null) dialog.show();
    }
}
