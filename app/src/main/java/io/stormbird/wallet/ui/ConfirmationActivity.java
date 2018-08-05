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
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ConfirmationType;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.FinishReceiver;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.viewmodel.ConfirmationViewModel;
import io.stormbird.wallet.viewmodel.ConfirmationViewModelFactory;
import io.stormbird.wallet.viewmodel.GasSettingsViewModel;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.token.entity.TicketRange;

import static io.stormbird.wallet.C.PRUNE_ACTIVITY;

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
    private Button sendButton;

    private BigInteger amount;
    private int decimals;
    private String contractAddress;
    private String amountStr;

    private ConfirmationType confirmationType;
    private boolean tokenTransfer;

    private List<TicketRange> salesOrderRange = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_confirm);
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
        sendButton.setOnClickListener(view -> onSend());

        String toAddress = getIntent().getStringExtra(C.EXTRA_TO_ADDRESS);
        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        confirmationType = ConfirmationType.values()[getIntent().getIntExtra(C.TOKEN_TYPE, 0)];
        amountStr = getIntent().getStringExtra(C.EXTRA_AMOUNT);
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, -1);
        String symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        String tokenList = getIntent().getStringExtra(C.EXTRA_TOKENID_LIST);
        String amountString;

        amount = new BigInteger(getIntent().getStringExtra(C.EXTRA_AMOUNT));

        switch (confirmationType) {
            case ETH:
                amountString = "-" + BalanceUtils.subunitToBase(amount, decimals).toPlainString() + " " + symbol;
                tokenTransfer = false;
                break;
            case ERC20:
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = "-" + BalanceUtils.subunitToBase(amount, decimals).toPlainString() + " " + symbol;
                tokenTransfer = true;
                break;
            case ERC875:
                contractAddrText.setVisibility(View.VISIBLE);
                contractAddrLabel.setVisibility(View.VISIBLE);
                contractAddrText.setText(contractAddress);
                amountString = tokenList;
                tokenTransfer = true;
                break;
            case MARKET:
                amountString = tokenList;
                toAddress = "Stormbird market";
                tokenTransfer = false;
                break;
            default:
                amountString = "-" + BalanceUtils.subunitToBase(amount, decimals).toPlainString() + " " + symbol;
                tokenTransfer = false;
                break;
        }

        toAddressText.setText(toAddress);

        valueText.setText(amountString);

        viewModel = ViewModelProviders.of(this, confirmationViewModelFactory)
                .get(ConfirmationViewModel.class);

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
//        getMenuInflater().inflate(R.menu.confirmation_menu, menu);
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
        viewModel.prepare(tokenTransfer);
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
                        amount,
                        gasSettings.gasPrice,
                        gasSettings.gasLimit);
                break;

            case ERC20:
                viewModel.createTokenTransfer(
                        fromAddressText.getText().toString(),
                        toAddressText.getText().toString(),
                        contractAddress,
                        amount,
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

            case MARKET:
                //price in eth
                BigInteger wei = Convert.toWei("2470", Convert.Unit.FINNEY).toBigInteger();
                viewModel.generateSalesOrders(amountStr, contractAddress, wei, valueText.getText().toString());
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

        String networkFee = BalanceUtils.weiToEth(new BigDecimal(gasSettings
                .gasPrice.multiply(gasSettings.gasLimit))).toPlainString() + " " + C.ETH_SYMBOL;
        networkFeeText.setText(networkFee);
    }

    private void onError(ErrorEnvelope error) {
        hideDialog();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.error_transaction_failed)
                .setMessage(error.message)
                .setPositiveButton(R.string.button_ok, (dialog1, id) -> {
                    // Do nothing
                })
                .create();
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
