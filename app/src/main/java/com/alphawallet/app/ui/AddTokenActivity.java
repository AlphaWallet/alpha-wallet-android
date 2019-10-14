package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QrUrlResult;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.QRURLParser;
import com.alphawallet.app.util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import dagger.android.AndroidInjection;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.app.C;
import com.alphawallet.app.R;

import com.alphawallet.app.viewmodel.AddTokenViewModel;
import com.alphawallet.app.viewmodel.AddTokenViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.InputAddressView;
import com.alphawallet.app.widget.InputView;

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
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

    private AWalletAlertDialog dialog;
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
    private QrUrlResult currentResult;

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
        viewModel.tokenFinalised().observe(this, this::onFetchedToken);
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

    private void onFetchedToken(Token token)
    {
        showProgress(false);
        if (token != null)
        {
            //got it, launch send screen
            viewModel.showSend(this, currentResult, token);
            finish();
        }
        else
        {
            onNoContractFound();
        }
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

    private void showCameraDenied()
    {
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.title_dialog_error);
        dialog.setMessage(R.string.error_camera_permission_denied);
        dialog.setIcon(ERROR);
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
                                     dialog.dismiss();
                                 });
        dialog.show();
    }

    private void finishAndLaunchSend()
    {
        //check if this is a token
        if (currentResult.getFunction().length() > 0)
        {
            //check we have this token
            Token token = viewModel.getToken(currentResult.chainId, currentResult.getAddress());
            if (token == null)
            {
                showProgress(true);
                //attempt to load the token and store to tokenService
                viewModel.fetchToken(currentResult.chainId, currentResult.getAddress());
                return;
            }
            else
            {
                viewModel.showSend(this, currentResult, token);
                finish();
            }
        }
        else
        {
            //launch send payment screen for eth transaction
            viewModel.showSend(this, currentResult, viewModel.getToken(currentResult.chainId, viewModel.wallet().getValue().address));
            finish();
        }
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
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(R.string.title_dialog_error);
        dialog.setMessage(R.string.error_add_token);
        dialog.setIcon(ERROR);
        dialog.setButtonText(R.string.try_again);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
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
            viewModel.testNetworks(address, networkInfo);
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
            viewModel.save(address, symbol, decimals, name, viewModel.getSelectedChain());
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
        if (networkInfo != null)
        {
            currentNetwork.setText(networkInfo.name);
            Utils.setChainColour(networkIcon, networkInfo.chainId);
            viewModel.setPrimaryChain(chainId);
        }
    }

    private void selectNetwork() {
        Intent intent = new Intent(AddTokenActivity.this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, String.valueOf(networkInfo.chainId));
        startActivityForResult(intent, C.REQUEST_SELECT_NETWORK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.REQUEST_SELECT_NETWORK && resultCode == RESULT_OK) {
            int networkId = data.getIntExtra(C.EXTRA_CHAIN_ID, 1);
            setupNetwork(networkId);
        }
        else if (requestCode == InputAddressView.BARCODE_READER_REQUEST_CODE) {
            switch (resultCode)
            {
                case FullScannerFragment.SUCCESS:
                    if (data != null) {
                        String barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                        QRURLParser parser = QRURLParser.getInstance();
                        currentResult = parser.parse(barcode);

                        String extracted_address = null;

                        if (currentResult != null)
                        {
                            extracted_address = currentResult.getAddress();
                            switch (currentResult.getProtocol())
                            {
                                case "address":
                                    break;
                                case "ethereum":
                                    //EIP681 protocol
                                    if (currentResult.chainId != 0 && extracted_address != null)
                                    {
                                        //this is a payment request
                                        finishAndLaunchSend();
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        else //try magiclink
                        {
                            ParseMagicLink magicParser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
                            try
                            {
                                if (magicParser.parseUniversalLink(barcode).chainId > 0) //see if it's a valid link
                                {
                                    //let's try to import the link
                                    viewModel.showImportLink(this, barcode);
                                    finish();
                                    return;
                                }
                            }
                            catch (SalesOrderMalformed e)
                            {
                                e.printStackTrace();
                            }
                        }

                        if (extracted_address == null) {
                            Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        inputAddressView.setAddress(extracted_address);
                    }
                    break;
                case QRScanningActivity.DENY_PERMISSION:
                    showCameraDenied();
                    break;
                default:
                    Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                                                "Code: " + String.valueOf(resultCode)
                    ));
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
