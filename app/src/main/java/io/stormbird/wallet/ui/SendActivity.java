package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.ui.widget.adapter.AutoCompleteUrlAdapter;
import io.stormbird.wallet.ui.widget.entity.AmountEntryItem;
import io.stormbird.wallet.ui.widget.entity.ENSHandler;
import io.stormbird.wallet.ui.widget.entity.ItemClickListener;
import io.stormbird.wallet.ui.zxing.FullScannerFragment;
import io.stormbird.wallet.ui.zxing.QRScanningActivity;
import io.stormbird.wallet.util.BalanceUtils;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.util.QRURLParser;
import io.stormbird.wallet.util.Utils;
import io.stormbird.wallet.viewmodel.SendViewModel;
import io.stormbird.wallet.viewmodel.SendViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static io.stormbird.token.tools.Convert.getEthString;
import static io.stormbird.wallet.C.Key.WALLET;

public class SendActivity extends BaseActivity implements Runnable, ItemClickListener, AmountUpdateCallback {
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    @Inject
    SendViewModelFactory sendViewModelFactory;
    SendViewModel viewModel;
    @Inject
    protected TokenRepositoryType tokenRepository;

    // In case we're sending tokens
    private boolean sendingTokens = false;
    private String myAddress;
    private int decimals;
    private String symbol;
    private Wallet wallet;
    private Token token;
    private String contractAddress;
    private ENSHandler ensHandler;
    private Handler handler;
    private AWalletAlertDialog dialog;
    private TextView chainName;
    private int currentChain;

    private ImageButton scanQrImageView;
    private TextView tokenBalanceText;
    private TextView tokenSymbolText;
    private AutoCompleteTextView toAddressEditText;
    private TextView pasteText;
    private Button nextBtn;
    private String currentAmount;

    private AmountEntryItem amountInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        toolbar();
        setTitle("");

        viewModel = ViewModelProviders.of(this, sendViewModelFactory)
                .get(SendViewModel.class);
        handler = new Handler();

        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, C.ETHER_DECIMALS);
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        sendingTokens = getIntent().getBooleanExtra(C.EXTRA_SENDING_TOKENS, false);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        QrUrlResult result = getIntent().getParcelableExtra(C.EXTRA_AMOUNT);
        myAddress = wallet.address;
        currentChain = token.tokenInfo.chainId;

        setupTokenContent();
        initViews();
        setupAddressEditField();

        if (token.addressMatches(myAddress)) {
            amountInput = new AmountEntryItem(this, tokenRepository, symbol, true, currentChain, token.hasRealValue());
        } else {
            //currently we don't evaluate ERC20 token value. TODO: Should we?
            amountInput = new AmountEntryItem(this, tokenRepository, symbol, false, currentChain, token.hasRealValue());
        }

        if (result != null)
        {
            //restore payment request
            validateEIP681Request(result);
        }
    }

    private void initViews() {

        toAddressEditText = findViewById(R.id.edit_to_address);

        pasteText = findViewById(R.id.paste);
        pasteText.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            try {
                CharSequence textToPaste = clipboard.getPrimaryClip().getItemAt(0).getText();
                toAddressEditText.setText(textToPaste);
            } catch (Exception e) {
                Log.e(SendActivity.class.getSimpleName(), e.getMessage(), e);
            }
        });

        nextBtn = findViewById(R.id.button_next);
        nextBtn.setOnClickListener(v -> {
            onNext();
        });

        scanQrImageView = findViewById(R.id.img_scan_qr);
        scanQrImageView.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRScanningActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });
    }

    private void setupAddressEditField() {
        AutoCompleteUrlAdapter adapterUrl = new AutoCompleteUrlAdapter(getApplicationContext(), C.ENS_HISTORY);
        adapterUrl.setListener(this);
        ENSCallback ensCallback = new ENSCallback() {
            @Override
            public void ENSComplete() {
                onNext();
            }

            @Override
            public void ENSCheck(String name) {
                viewModel.checkENSAddress(token.tokenInfo.chainId, name);
            }
        };
        ensHandler = new ENSHandler(this, handler, adapterUrl, this, ensCallback);
        viewModel.ensResolve().observe(this, ensHandler::onENSSuccess);
        viewModel.ensFail().observe(this, ensHandler::hideENS);
    }

    private void onNext() {
        KeyboardUtils.hideKeyboard(getCurrentFocus());
        boolean isValid = amountInput.checkValidAmount();

        if (!isBalanceEnough(currentAmount)) {
            amountInput.setError(R.string.error_insufficient_funds);
            isValid = false;
        }

        String to = ensHandler.getAddressFromEditView();
        if (to == null) return;

        if (isValid) {
            BigInteger amountInSubunits = BalanceUtils.baseToSubunit(currentAmount, decimals);
            viewModel.openConfirmation(this, to, amountInSubunits, contractAddress, decimals, symbol, sendingTokens, ensHandler.getEnsName(), currentChain);
        }
    }

    private void onBack() {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBack();
                break;
            }
            case R.id.action_qr:
                viewModel.showContractInfo(this, wallet, token);
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        onBack();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == FullScannerFragment.SUCCESS) {
                if (data != null) {
                    String barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                    //if barcode is still null, ensure we don't GPF
                    if (barcode == null) {
                        //Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        displayScanError();
                        return;
                    }

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
                                validateEIP681Request(result);
                                break;
                            default:
                                break;
                        }

                        toAddressEditText.setText(extracted_address);
                    }

                    if (extracted_address == null)
                    {
                        displayScanError();
                    }
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

    private void validateEIP681Request(QrUrlResult result)
    {
        //check chain
        if (result.chainId == 0)
        {
            displayScanError();
            return;
        }
        else
        {
            //chain ID indicator
            Utils.setChainColour(chainName, result.chainId);
            chainName.setText(viewModel.getChainName(result.chainId));
            currentChain = result.chainId;
            amountInput.onClear();
        }

        Token resultToken = viewModel.getToken(result.chainId, result.getAddress());

        if (result.getFunction().length() == 0 && result.weiValue.compareTo(BigInteger.ZERO) > 0)
        {
            //correct chain and asset type
            String ethAmount = BalanceUtils.weiToEth(new BigDecimal(result.weiValue)).setScale(4, RoundingMode.HALF_DOWN).stripTrailingZeros().toPlainString();
            TextView sendText = findViewById(R.id.text_payment_request);
            sendText.setVisibility(View.VISIBLE);
            sendText.setText(R.string.transfer_request);
            token = viewModel.getToken(result.chainId, wallet.address);
            toAddressEditText.setText(result.getAddress());
            amountInput = new AmountEntryItem(this, tokenRepository, viewModel.getNetworkInfo(result.chainId).symbol, true, result.chainId, EthereumNetworkRepository.hasRealValue(result.chainId));
            amountInput.setAmountText(ethAmount);
            amountInput.setAmount(ethAmount);
            setupTokenContent();
        }
        else if (result.getFunction().length() > 0 && resultToken != null && !resultToken.isEthereum())
        {
            //sending ERC20 and we're on the correct asset
            BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, resultToken.tokenInfo.decimals));
            BigDecimal sendAmount = new BigDecimal(result.weiValue);
            BigDecimal erc20Amount = resultToken.tokenInfo.decimals > 0
                    ? sendAmount.divide(decimalDivisor) : sendAmount;
            String erc20AmountStr = erc20Amount.setScale(4, RoundingMode.HALF_DOWN).stripTrailingZeros().toPlainString();
            amountInput = new AmountEntryItem(this, tokenRepository, resultToken.tokenInfo.symbol, false, result.chainId, EthereumNetworkRepository.hasRealValue(result.chainId));
            amountInput.setAmount(erc20AmountStr);

            //show function which will be called:
            TextView sendText = findViewById(R.id.text_payment_request);
            sendText.setVisibility(View.VISIBLE);
            sendText.setText(R.string.token_transfer_request);

            TextView contractText = findViewById(R.id.text_contract_call);
            contractText.setVisibility(View.VISIBLE);
            contractText.setText(result.functionDetail);
        }
    }

    private void displayScanError()
    {
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(R.string.toast_qr_code_no_address);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void displayScanError(int titleId, String message)
    {
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(AWalletAlertDialog.ERROR);
        dialog.setTitle(titleId);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.dialog_ok);
        dialog.setButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        amountInput.onClear();
    }

    private boolean isBalanceEnough(String eth) {
        try {
            //Needs to take into account decimal of token
            int decimals = (token != null && token.tokenInfo != null) ? token.tokenInfo.decimals : 18;
            BigDecimal amount = new BigDecimal(BalanceUtils.baseToSubunit(eth, decimals));
            return (token.balance.subtract(amount).compareTo(BigDecimal.ZERO) >= 0);
        } catch (Exception e) {
            return false;
        }
    }

    public void setupTokenContent() {
        tokenBalanceText = findViewById(R.id.balance_eth);
        tokenSymbolText = findViewById(R.id.symbol);
        chainName = findViewById(R.id.text_chain_name);

        tokenSymbolText.setText(TextUtils.isEmpty(token.tokenInfo.name)
                ? token.tokenInfo.symbol.toUpperCase()
                : getString(R.string.token_name, token.tokenInfo.name, token.tokenInfo.symbol.toUpperCase()));

        TokenInfo tokenInfo = token.tokenInfo;
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? token.balance.divide(decimalDivisor) : token.balance;
        ethBalance = ethBalance.setScale(4, RoundingMode.HALF_DOWN).stripTrailingZeros();
        String value = getEthString(ethBalance.doubleValue());
        tokenBalanceText.setText(value);

        tokenBalanceText.setVisibility(View.VISIBLE);
        if (token != null)
        {
            Utils.setChainColour(chainName, token.tokenInfo.chainId);
            chainName.setText(viewModel.getChainName(token.tokenInfo.chainId));
        }
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
    public void amountChanged(String newAmount)
    {
        currentAmount = newAmount;
    }
}
