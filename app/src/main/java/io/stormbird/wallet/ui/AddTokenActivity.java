package io.stormbird.wallet.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.QrUrlResult;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.ui.zxing.FullScannerFragment;
import io.stormbird.wallet.util.QRURLParser;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.AddTokenViewModel;
import io.stormbird.wallet.viewmodel.AddTokenViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.InputAddressView;
import io.stormbird.wallet.widget.InputView;
import io.stormbird.wallet.widget.SelectNetworkDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.stormbird.wallet.C.ADDED_TOKEN;
import static org.web3j.crypto.WalletUtils.isValidAddress;

public class AddTokenActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    protected AddTokenViewModelFactory addTokenViewModelFactory;
    private AddTokenViewModel viewModel;

    private final Pattern findAddress = Pattern.compile("(0x)([0-9a-fA-F]{40})($|\\s)");

    public LinearLayout ticketLayout;

    //Ticket Info
    public TextView venue;
    public TextView date;
    public TextView price;

    private Dialog dialog;
    private String lastCheck;

    LinearLayout progressLayout;

    public InputAddressView inputAddressView;
    public InputView symbolInputView;
    public InputView decimalsInputView;
    public InputView nameInputview;
    private String contractAddress;
    private View networkIcon;
    private NetworkInfo networkInfo;
    private TextView currentNetwork;
    private RelativeLayout selectNetworkLayout;
    public TextView chainName;

    private AWalletAlertDialog aDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_token);

        toolbar();

        symbolInputView = findViewById(R.id.input_symbol);
        decimalsInputView = findViewById(R.id.input_decimal);
        nameInputview = findViewById(R.id.input_name);
        chainName = symbolInputView.findViewById(R.id.text_chain_name);

        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        currentNetwork = findViewById(R.id.current_network);
        networkIcon = findViewById(R.id.network_icon);
        selectNetworkLayout = findViewById(R.id.select_network_layout);
        selectNetworkLayout.setOnClickListener(v -> selectNetwork());

        selectNetworkLayout.setVisibility(View.VISIBLE);

        progressLayout = findViewById(R.id.layout_progress);

        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);

        ticketLayout = findViewById(R.id.layoutTicket);

        findViewById(R.id.save).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, addTokenViewModelFactory)
                .get(AddTokenViewModel.class);
        viewModel.error().observe(this, this::onError);
        viewModel.result().observe(this, this::onSaved);
        viewModel.update().observe(this, this::onChecked);
        viewModel.switchNetwork().observe(this, this::setupNetwork);
        lastCheck = "";

        inputAddressView = findViewById(R.id.input_address_view);
        inputAddressView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //wait until we have an ethereum address
                String check = inputAddressView.getAddress().toLowerCase();
                //process the address first
                if (check.length() > 39) {
                    onCheck(check);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        setTitle(R.string.empty);

        setupNetwork(1);
        viewModel.prepare();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (contractAddress != null)
        {
            inputAddressView.setAddress(contractAddress.toLowerCase());
            contractAddress = null;
        }
    }

    private void showProgress(Boolean shouldShowProgress) {
        if (shouldShowProgress) {
            progressLayout.setVisibility(View.VISIBLE);
        } else {
            progressLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == InputAddressView.BARCODE_READER_REQUEST_CODE) {
            if (resultCode == FullScannerFragment.SUCCESS) {
                if (data != null) {
                    String barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                    QRURLParser parser = QRURLParser.getInstance();
                    QrUrlResult result = parser.parse(barcode);

                    String extracted_address = null;

                    if (result != null)
                    {
                        extracted_address = result.getAddress();
                        switch (result.getProtocol())
                        {
                            case "address":
                                break;
                            case "ethereum":
                                //EIP681 protocol
                                if (result.chainId != 0 && extracted_address != null)
                                {
                                    //this is a payment request
                                    finishAndLaunchSend(result);
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    if (extracted_address == null) {
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    inputAddressView.setAddress(extracted_address);
                }
            } else {
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                        "Code: " + String.valueOf(resultCode)
                        ));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void finishAndLaunchSend(QrUrlResult result)
    {
        //launch send payment screen
        viewModel.showSend(this, result);
        finish();
    }

    private void onSaved(boolean result)
    {
        if (result)
        {
            sendBroadcast(new Intent(ADDED_TOKEN)); //inform walletview there is a new token
            finish();
        }
    }

    private void onChecked(boolean found)
    {
        showProgress(false);
        if (found)
        {
            TokenInfo token = viewModel.tokenInfo().getValue();
            token.addTokenSetupPage(this, viewModel.getNetworkInfo(token.chainId).getShortName());
        }
        else
        {
            chainName.setVisibility(View.GONE);
            onNoContractFound();
        }
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_dialog_error)
                .setMessage(R.string.error_add_token)
                .setPositiveButton(R.string.try_again, null)
                .create();
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save: {
                onSave();
            } break;
        }
    }

    private void onCheck(String address)
    {
        if (!isValidAddress(address))
        {
            //if it's not a valid address is there something that appears to be an address in here?
            Matcher matcher = findAddress.matcher(address);
            if (matcher.find())
            {
                address = matcher.group(1) + matcher.group(2);
            }
        }

        if (isValidAddress(address) && !address.equals(lastCheck))
        {
            lastCheck = address;
            chainName.setVisibility(View.GONE);
            showProgress(true);
            viewModel.testNetworks(address);
        }
    }

    private void onSave() {
        boolean isValid = true;
        String address = inputAddressView.getAddress();
        String symbol = symbolInputView.getText().toString().toLowerCase();
        String rawDecimals = decimalsInputView.getText().toString();
        String name = nameInputview.getText().toString();

        int decimals = 0;

        if (TextUtils.isEmpty(address)) {
            inputAddressView.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(symbol)) {
            symbolInputView.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(rawDecimals)) {
            decimalsInputView.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        try {
            decimals = Integer.valueOf(rawDecimals);
        } catch (NumberFormatException ex) {
            decimalsInputView.setError(getString(R.string.error_must_numeric));
            isValid = false;
        }

        if (!isValidAddress(address)) {
            inputAddressView.setError(getString(R.string.error_invalid_address));
            isValid = false;
        }

        if (isValid) {
            viewModel.save(address, symbol, decimals, name, networkInfo.chainId);
            showProgress(true);
        }
    }

    private void onNoContractFound()
    {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.no_token_found_title);
        aDialog.setIcon(AWalletAlertDialog.NONE);
        aDialog.setMessage(R.string.no_token_found);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
    }

    private void setupNetwork(int chainId)
    {
        networkInfo = viewModel.getNetworkInfo(chainId);
        currentNetwork.setText(networkInfo.name);
        Utils.setChainColour(networkIcon, networkInfo.chainId);
        viewModel.setPrimaryChain(chainId);
    }

    private void selectNetwork() {
        SelectNetworkDialog dialog = new SelectNetworkDialog(this, viewModel.getNetworkList(), String.valueOf(networkInfo.chainId), true);
        dialog.setOnClickListener(v1 -> {
            networkInfo = viewModel.getNetwork(dialog.getSelectedChainId());
            if (networkInfo != null) setupNetwork(networkInfo.chainId);
            dialog.dismiss();
        });
        dialog.show();
    }
}
