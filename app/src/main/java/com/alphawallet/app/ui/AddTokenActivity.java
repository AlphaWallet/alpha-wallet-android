package com.alphawallet.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.ui.zxing.FullScannerFragment;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.AddTokenViewModel;
import com.alphawallet.app.viewmodel.AddTokenViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputAddress;
import com.alphawallet.app.widget.InputView;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.repository.SharedPreferenceRepository.HIDE_ZERO_BALANCE_TOKENS;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;

public class AddTokenActivity extends BaseActivity implements AddressReadyCallback, StandardFunctionInterface
{
    @Inject
    protected AddTokenViewModelFactory addTokenViewModelFactory;
    private AddTokenViewModel viewModel;

    private final Pattern findAddress = Pattern.compile("(0x)([0-9a-fA-F]{40})($|\\s)");

    public LinearLayout ticketLayout;

    //Ticket Info
    public TextView venue;
    public TextView date;
    public TextView price;

    private String lastCheck;

    LinearLayout progressLayout;

    public InputAddress inputAddressView;
    public InputView symbolInputView;
    public InputView decimalsInputView;
    public InputView nameInputview;
    private String contractAddress;
    private View networkIcon;
    private NetworkInfo networkInfo;
    private TextView currentNetwork;
    private RelativeLayout selectNetworkLayout;
    private QRResult currentResult;
    private InputView tokenType;
    private ContractType contractType;
    private boolean zeroBalanceToken = false;

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

        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        currentNetwork = findViewById(R.id.current_network);
        networkIcon = findViewById(R.id.network_icon);
        tokenType = findViewById(R.id.input_token_type);
        selectNetworkLayout = findViewById(R.id.select_network_layout);
        selectNetworkLayout.setOnClickListener(v -> selectNetwork());
        selectNetworkLayout.setVisibility(View.VISIBLE);
        tokenType.setVisibility(View.GONE);

        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_save)));
        functionBar.revealButtons();

        progressLayout = findViewById(R.id.layout_progress);

        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);
        contractType = null;

        ticketLayout = findViewById(R.id.layoutTicket);

        viewModel = new ViewModelProvider(this, addTokenViewModelFactory)
                .get(AddTokenViewModel.class);
        viewModel.error().observe(this, this::onError);
        viewModel.result().observe(this, this::onSaved);
        viewModel.noContract().observe(this, this::onNoContractFound);
        viewModel.tokenFinalised().observe(this, this::onFetchedToken);
        viewModel.switchNetwork().observe(this, this::setupNetwork);
        viewModel.tokenType().observe(this, this::onTokenType);
        viewModel.tokenInfo().observe(this, this::onTokenInfo);
        lastCheck = "";

        inputAddressView = findViewById(R.id.input_address_view);
        inputAddressView.setAddressCallback(this);

        inputAddressView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //wait until we have an ethereum address
                String check = inputAddressView.getInputText().toLowerCase().trim();
                //process the address first
                if (check.length() > 38) {
                    onCheck(check);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        setTitle(R.string.empty);

        setupNetwork(EthereumNetworkRepository.getOverrideToken().chainId);
        viewModel.prepare();
    }

    private void onTokenType(Token contractTypeToken)
    {
        tokenType = findViewById(R.id.input_token_type);
        showProgress(false);
        if (contractTypeToken.getInterfaceSpec() != ContractType.OTHER)
        {
            if (!contractTypeToken.hasPositiveBalance()) zeroBalanceToken = true;
            tokenType.setVisibility(View.VISIBLE);
            String showBalance = contractTypeToken.getStringBalance() + " " + contractTypeToken.getInterfaceSpec().toString();
            tokenType.setText(showBalance);
            contractType = contractTypeToken.getInterfaceSpec();
        }
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
            onNoContractFound(true);
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
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setMessage(R.string.error_camera_permission_denied);
        aDialog.setIcon(ERROR);
        aDialog.setButtonText(R.string.button_ok);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
                                 });
        aDialog.show();
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

    private void onSaved(Token result)
    {
        showProgress(false);
        if (result != null)
        {
            ContractLocator cr = new ContractLocator(result.getAddress(), result.tokenInfo.chainId);
            Intent intent = new Intent(ADDED_TOKEN);
            intent.putParcelableArrayListExtra(C.EXTRA_TOKENID_LIST, new ArrayList<>(Collections.singletonList(cr)));
            sendBroadcast(intent);
            finish();
        }
    }

    private void onTokenInfo(TokenInfo tokenInfo)
    {
        tokenInfo.addTokenSetupPage(this, viewModel.getNetworkInfo(tokenInfo.chainId).getShortName());
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setMessage(R.string.error_add_token);
        aDialog.setIcon(ERROR);
        aDialog.setButtonText(R.string.try_again);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
        });
        aDialog.show();
    }

    @Override
    public void handleClick(String action, int id)
    {
        onSave();
    }

    private void onCheck(String address)
    {
        if (!Utils.isAddressValid(address))
        {
            //if it's not a valid address is there something that appears to be an address in here?
            Matcher matcher = findAddress.matcher(address);
            if (matcher.find())
            {
                address = matcher.group(1) + matcher.group(2);
            }
        }

        if (Utils.isAddressValid(address) && !address.equals(lastCheck))
        {
            lastCheck = address;
            showProgress(true);
            viewModel.testNetworks(address, networkInfo);
        }
    }

    private void saveFinal(String address)
    {
        boolean isValid = true;
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
            decimals = Integer.parseInt(rawDecimals);
        } catch (NumberFormatException ex) {
            decimalsInputView.setError(getString(R.string.error_must_numeric));
            isValid = false;
        }

        if (!Utils.isAddressValid(address)) {
            inputAddressView.setError(getString(R.string.error_invalid_address));
            isValid = false;
        }

        if (isValid)
        {
            showProgress(true);
            viewModel.save(viewModel.getSelectedChain(), address, name, symbol, decimals, contractType);
        }
    }

    private void onSave() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (zeroBalanceToken && pref.getBoolean(HIDE_ZERO_BALANCE_TOKENS, false))
        {
            userAddingZeroBalanceToken();
        }
        else
        {
            inputAddressView.getAddress();
        }
    }

    private void userAddingZeroBalanceToken()
    {
        //warn user we are switching on zero balance tokens
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.zero_balance_tokens_off);
        aDialog.setIcon(AWalletAlertDialog.WARNING);
        aDialog.setMessage(R.string.zero_balance_tokens_are_switched_off);
        aDialog.setButtonText(R.string.dialog_switch_zero_balance_tokens_on);
        aDialog.setButtonListener(v -> {
            aDialog.dismiss();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            pref.edit().putBoolean(HIDE_ZERO_BALANCE_TOKENS, false).apply();
            inputAddressView.getAddress();
        });
        aDialog.setSecondaryButtonText(R.string.action_cancel);
        aDialog.setSecondaryButtonListener(v -> {
            //don't switch on the zero balance tokens
            aDialog.dismiss();
            inputAddressView.getAddress();
        });
        aDialog.show();
    }

    private void onNoContractFound(Boolean noContract)
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

    ActivityResultLauncher<Intent> getNetwork = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int networkId = result.getData().getIntExtra(C.EXTRA_CHAIN_ID, 1);
                setupNetwork(networkId);
            });

    private void selectNetwork() {
        Intent intent = new Intent(AddTokenActivity.this, SelectNetworkActivity.class);
        intent.putExtra(C.EXTRA_LOCAL_NETWORK_SELECT_FLAG, true);
        intent.putExtra(C.EXTRA_CHAIN_ID, networkInfo.chainId);
        getNetwork.launch(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.BARCODE_READER_REQUEST_CODE) {
            switch (resultCode)
            {
                case FullScannerFragment.SUCCESS:
                    if (data != null) {
                        String barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                        QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
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
                            catch (Exception e)
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

    @Override
    public void addressReady(String address, String ensName)
    {
        saveFinal(address);
    }
}
