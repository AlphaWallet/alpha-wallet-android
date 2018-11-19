package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ConfirmationType;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.viewmodel.ConfirmationViewModel;
import io.stormbird.wallet.viewmodel.ConfirmationViewModelFactory;
import io.stormbird.wallet.viewmodel.GasSettingsViewModel;
import io.stormbird.wallet.web3.entity.Web3Transaction;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.token.tools.Convert.getEthString;
import static io.stormbird.wallet.C.ETH_SYMBOL;
import static io.stormbird.wallet.C.PRUNE_ACTIVITY;
import static io.stormbird.wallet.entity.ConfirmationType.WEB3TRANSACTION;
import static io.stormbird.wallet.widget.AWalletAlertDialog.ERROR;

public class ConfirmationActivity extends BaseActivity {
    AWalletAlertDialog dialog;

    @Inject
    ConfirmationViewModelFactory confirmationViewModelFactory;
    ConfirmationViewModel viewModel;

    private FinishReceiver finishReceiver;

//    private SystemView systemView;
//    private ProgressView progressView;

    private TextView fromAddressText;
    private TextView toAddressText;
    private TextView valueText;
    private TextView gasPriceText;
    private TextView gasLimitText;
    private TextView networkFeeText;
    private TextView contractAddrText;
    private TextView contractAddrLabel;
    private TextView websiteLabel;
    private TextView websiteText;
    private Button sendButton;
    private TextView title;
    private TextView labelAmount;

    private BigDecimal amount;
    private int decimals;
    private String contractAddress;
    private String amountStr;

    private ConfirmationType confirmationType;
    private byte[] transactionBytes = null;
    private Web3Transaction transaction;
    private boolean isMainNet;
    private String networkName;

    private List<TicketRange> salesOrderRange = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_confirm);
        transaction = null;
        toolbar();

        setTitle("");
        fromAddressText = findViewById(R.id.text_from);
        toAddressText = findViewById(R.id.text_to);
        valueText = findViewById(R.id.text_value);
        gasPriceText = findViewById(R.id.text_gas_price);
        gasLimitText = findViewById(R.id.text_gas_limit);
        networkFeeText = findViewById(R.id.text_network_fee);
        sendButton = findViewById(R.id.send_button);
        contractAddrText = findViewById(R.id.text_contract);
        contractAddrLabel = findViewById(R.id.label_contract);
        websiteLabel = findViewById(R.id.label_website);
        websiteText = findViewById(R.id.text_website);
        title = findViewById(R.id.title_confirm);
        labelAmount = findViewById(R.id.label_amount);
        sendButton.setOnClickListener(view -> onSend());

        transaction = getIntent().getParcelableExtra(C.EXTRA_WEB3TRANSACTION);

        String toAddress = getIntent().getStringExtra(C.EXTRA_TO_ADDRESS);
        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        confirmationType = ConfirmationType.values()[getIntent().getIntExtra(C.TOKEN_TYPE, 0)];
        String ensName = getIntent().getStringExtra(C.EXTRA_ENS_DETAILS);
        amountStr = getIntent().getStringExtra(C.EXTRA_AMOUNT);
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, -1);
        String symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        String tokenList = getIntent().getStringExtra(C.EXTRA_TOKENID_LIST);
        String amountString;

        amount = new BigDecimal(getIntent().getStringExtra(C.EXTRA_AMOUNT));

        viewModel = ViewModelProviders.of(this, confirmationViewModelFactory)
                .get(ConfirmationViewModel.class);

        switch (confirmationType) {
            case ETH:
                amountString = "-" + BalanceUtils.subunitToBase(amount.toBigInteger(), decimals).toPlainString() + " " + symbol;
                transactionBytes = null;
                break;
            case ERC20:
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = "-" + BalanceUtils.subunitToBase(amount.toBigInteger(), decimals).toPlainString() + " " + symbol;
                transactionBytes = TokenRepository.createTokenTransferData(toAddress, amount.toBigInteger());
                break;
            case ERC875:
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = tokenList;
                transactionBytes = viewModel.getERC875TransferBytes(toAddress, contractAddress, amountStr);
                break;
            case MARKET:
                amountString = tokenList;
                toAddress = "Stormbird market";
                break;
            case WEB3TRANSACTION:
                title.setText(R.string.confirm_dapp_transaction);
                toAddress = transaction.recipient.toString();
                if (transaction.contract != null)
                {
                    contractAddrText.setVisibility(View.VISIBLE);
                    contractAddrLabel.setVisibility(View.VISIBLE);
                    contractAddrText.setText(transaction.contract.toString());
                }
                else
                {
                    BigInteger addr = Numeric.toBigInt(transaction.recipient.toString());
                    if (addr.equals(BigInteger.ZERO)) //constructor
                    {
                        toAddress = getString(R.string.ticket_contract_constructor);
                    }
                }
                String urlRequester = getIntent().getStringExtra(C.EXTRA_CONTRACT_NAME);
                networkName = getIntent().getStringExtra(C.EXTRA_NETWORK_NAME);
                isMainNet = getIntent().getBooleanExtra(C.EXTRA_NETWORK_MAINNET, false);

                if (urlRequester != null)
                {
                    websiteLabel.setVisibility(View.VISIBLE);
                    websiteText.setVisibility(View.VISIBLE);
                    websiteText.setText(urlRequester);
                }

                BigDecimal ethAmount = Convert.fromWei(transaction.value.toString(10), Convert.Unit.ETHER);
                amountString = getEthString(ethAmount.doubleValue()) + " " + ETH_SYMBOL;
                transactionBytes = Numeric.hexStringToByteArray(transaction.payload);
                break;
            case ERC721:
                String contractName = getIntent().getStringExtra(C.EXTRA_CONTRACT_NAME);
                title.setText(R.string.confirm_erc721_transfer);
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                String contractTxt = contractAddress + " " + contractName;
                labelAmount.setText(R.string.asset_name);
                contractAddrText.setText(contractTxt);
                amountString = symbol;
                transactionBytes = viewModel.getERC721TransferBytes(toAddress, contractAddress, amountStr);
                break;
            default:
                amountString = "-" + BalanceUtils.subunitToBase(amount.toBigInteger(), decimals).toPlainString() + " " + symbol;
                transactionBytes = null;
                break;
        }

        if (ensName != null && ensName.length() > 0)
        {
            toAddressText.setText(ensName);
        }
        else
        {
            toAddressText.setText(toAddress);
        }

        valueText.setText(amountString);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.gasSettings().observe(this, this::onGasSettings);
        viewModel.sendTransaction().observe(this, this::onTransaction);
        viewModel.progress().observe(this, this::onProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.pushToast().observe(this, this::displayToast);
        finishReceiver = new FinishReceiver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                viewModel.openGasSettings(ConfirmationActivity.this);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare(transactionBytes);
    }

    private void onProgress(boolean shouldShowProgress) {
        hideDialog();
        if (shouldShowProgress) {
            dialog = new AWalletAlertDialog(this);
            dialog.setProgressMode();
            dialog.setTitle(R.string.title_dialog_sending);
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideDialog();
        unregisterReceiver(finishReceiver);
    }

    private void onSend() {
        GasSettings gasSettings = viewModel.gasSettings().getValue();

        switch (confirmationType) {
            case ETH:
                viewModel.createTransaction(
                        fromAddressText.getText().toString(),
                        toAddressText.getText().toString(),
                        amount.toBigInteger(),
                        gasSettings.gasPrice,
                        gasSettings.gasLimit);
                break;

            case ERC20:
                viewModel.createTokenTransfer(
                        fromAddressText.getText().toString(),
                        toAddressText.getText().toString(),
                        contractAddress,
                        amount.toBigInteger(),
                        gasSettings.gasPrice,
                        gasSettings.gasLimit);
                break;

            case ERC875:
                viewModel.createTicketTransfer(
                        fromAddressText.getText().toString(),
                        toAddressText.getText().toString(),
                        contractAddress,
                        amountStr,
                        gasSettings.gasPrice,
                        gasSettings.gasLimit);
                break;

            case WEB3TRANSACTION:
                viewModel.signWeb3DAppTransaction(transaction, gasSettings.gasPrice, gasSettings.gasLimit);
                break;

            case MARKET:
                //price in eth
                BigInteger wei = Convert.toWei("2470", Convert.Unit.FINNEY).toBigInteger();
                viewModel.generateSalesOrders(amountStr, contractAddress, wei, valueText.getText().toString());
                break;

            case ERC721:
                viewModel.createERC721Transfer(
                        toAddressText.getText().toString(),
                        contractAddress,
                        amountStr,
                        gasSettings.gasPrice,
                        gasSettings.gasLimit);
                break;

            default:
                break;
        }
    }

    private void onDefaultWallet(Wallet wallet) {
        fromAddressText.setText(wallet.address);
    }

    private void onTransaction(String hash) {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.transaction_succeeded);
        dialog.setMessage(hash);
        dialog.setButtonText(R.string.copy);
        dialog.setButtonListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("transaction hash", hash);
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
    }

    private void onGasSettings(GasSettings gasSettings) {
        String gasPrice = BalanceUtils.weiToGwei(gasSettings.gasPrice) + " " + C.GWEI_UNIT;
        gasPriceText.setText(gasPrice);
        gasLimitText.setText(gasSettings.gasLimit.toString());

        BigDecimal networkFeeBD = new BigDecimal(gasSettings
                                                         .gasPrice.multiply(gasSettings.gasLimit));

        String networkFee = BalanceUtils.weiToEth(networkFeeBD).toPlainString() + " " + C.ETH_SYMBOL;
        networkFeeText.setText(networkFee);

        if (confirmationType == WEB3TRANSACTION)
        {
            //update amount
            BigDecimal ethValueBD = amount.add(networkFeeBD);

            //convert to ETH
            ethValueBD = Convert.fromWei(ethValueBD, Convert.Unit.ETHER);
            String valueUpdate = getEthString(ethValueBD.doubleValue()) + " " + ETH_SYMBOL;
            valueText.setText(valueUpdate);
        }
    }

    private void onError(ErrorEnvelope error) {
        hideDialog();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(error.message);
        dialog.setIcon(ERROR);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == GasSettingsViewModel.SET_GAS_SETTINGS) {
            if (resultCode == RESULT_OK) {
                BigInteger gasPrice = new BigInteger(intent.getStringExtra(C.EXTRA_GAS_PRICE));
                BigInteger gasLimit = new BigInteger(intent.getStringExtra(C.EXTRA_GAS_LIMIT));
                GasSettings settings = new GasSettings(gasPrice, gasLimit);
                viewModel.gasSettings().postValue(settings);
            }
        }
    }
}
