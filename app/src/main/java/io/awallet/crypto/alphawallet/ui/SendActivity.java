package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Address;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.router.EthereumInfoRouter;
import io.awallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import io.awallet.crypto.alphawallet.util.BalanceUtils;
import io.awallet.crypto.alphawallet.util.QRURLParser;
import io.awallet.crypto.alphawallet.util.QRUtils;
import io.awallet.crypto.alphawallet.viewmodel.SendViewModel;
import io.awallet.crypto.alphawallet.viewmodel.SendViewModelFactory;
import io.awallet.crypto.alphawallet.widget.AWalletAlertDialog;

import static io.awallet.crypto.alphawallet.C.Key.WALLET;

public class SendActivity extends BaseActivity {
    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;
    private static final String KEY_ADDRESS = "key_address";
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    @Inject
    SendViewModelFactory sendViewModelFactory;
    SendViewModel viewModel;

    // In case we're sending tokens
    private boolean sendingTokens = false;
    private String myAddress;
    private int decimals;
    private String symbol;
    private Wallet wallet;
    private Token token;

    RelativeLayout ethDetailLayout;
    Button startTransferButton;
    Button copyAddressButton;
    EditText amountEditText;
    EditText toAddressEditText;
    ImageView qrImageView;
    ImageButton scanQrImageView;
    TextView toAddressError;
    TextView amountError;
    TextView myAddressText;
    AWalletAlertDialog dialog;

    //Token
    TextView balanceEth;
    TextView textUsdValue;
    TextView symbolText;
    ImageView icon;
    TextView arrayBalance;
    TextView text24Hours;
    TextView textAppreciation;
    TextView issuer;
    TextView text24HoursSub;
    TextView textAppreciationSub;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send);
        toolbar();
        setTitle("");

        viewModel = ViewModelProviders.of(this, sendViewModelFactory)
                .get(SendViewModel.class);

        myAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, C.ETHER_DECIMALS);
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        sendingTokens = getIntent().getBooleanExtra(C.EXTRA_SENDING_TOKENS, false);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        String toAddress = getIntent().getStringExtra(C.EXTRA_ADDRESS);

        setupTokenContent();

        initViews();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            qrImageView = findViewById(R.id.qr_image);
            qrImageView.setImageBitmap(QRUtils.createQRImage(this, myAddress, qrImageView.getWidth()));
            qrImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
    }

    private void initViews() {
        toAddressError = findViewById(R.id.to_address_error);
        amountError = findViewById(R.id.amount_error);
        myAddressText = findViewById(R.id.address);
        myAddressText.setText(myAddress);
        ethDetailLayout = findViewById(R.id.layout_eth_detail);

        startTransferButton = findViewById(R.id.button_start_transfer);
        startTransferButton.setOnClickListener(v -> onStartTransfer());

        copyAddressButton = findViewById(R.id.copy_action);
        copyAddressButton.setOnClickListener(v -> copyAddress());

        scanQrImageView = findViewById(R.id.img_scan_qr);
        scanQrImageView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });

        amountEditText = findViewById(R.id.edit_amount);
        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                amountError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        toAddressEditText = findViewById(R.id.edit_to_address);
        toAddressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                toAddressError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void copyAddress() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, wallet.address);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private void onStartTransfer() {
        boolean isValid = true;

        dismissKeyboard();
        amountError.setVisibility(View.GONE);
        final String amount = amountEditText.getText().toString();
        if (!isValidAmount(amount) || !isBalanceEnough(amount)) {
            amountError.setVisibility(View.VISIBLE);
            amountError.setText(R.string.error_invalid_amount);
            isValid = false;
        }

        toAddressError.setVisibility(View.GONE);
        final String to = toAddressEditText.getText().toString();
        if (!isAddressValid(to)) {
            toAddressError.setVisibility(View.VISIBLE);
            toAddressError.setText(getString(R.string.error_invalid_address));
            isValid = false;
        }

        if (isValid) {
            BigInteger amountInSubunits = BalanceUtils.baseToSubunit(amountEditText.getText().toString(), decimals);
            viewModel.openConfirmation(this, to, amountInSubunits, myAddress, decimals, symbol, sendingTokens);
        }
    }

    private void onBack() {
        if (ethDetailLayout.getVisibility() == View.VISIBLE) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBack();
                break;
            }
            case R.id.action_info: {
                new EthereumInfoRouter().open(this);
                break;
            }
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
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

                    QRURLParser parser = QRURLParser.getInstance();
                    String extracted_address = parser.extractAddressFromQrString(barcode.displayValue);
                    if (extracted_address == null) {
                        dialog = new AWalletAlertDialog(this);
                        dialog.setIcon(AWalletAlertDialog.ERROR);
                        dialog.setTitle(R.string.toast_qr_code_no_address);
                        dialog.setButtonText(R.string.dialog_ok);
                        dialog.setButtonListener(v -> dialog.dismiss());
                        dialog.show();
                        return;
                    }
                    toAddressEditText.setText(extracted_address);
                }
            } else {
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    boolean isAddressValid(String address) {
        try {
            new Address(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isValidAmount(String eth) {
        try {
            String wei = BalanceUtils.EthToWei(eth);
            return wei != null;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isBalanceEnough(String eth) {
        try {
            BigDecimal amount = new BigDecimal(BalanceUtils.EthToWei(eth));
            BigDecimal balance = new BigDecimal(BalanceUtils.EthToWei(balanceEth.getText().toString()));
            return (balance.subtract(amount).compareTo(BigDecimal.ZERO) == 0 || balance.subtract(amount).compareTo(BigDecimal.ZERO) > 0);
        } catch (Exception e) {
            return false;
        }
    }

    private void dismissKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    public void setupTokenContent() { /* This method is copied from Token.java */
        icon = findViewById(R.id.icon);
        balanceEth = findViewById(R.id.balance_eth);
        textUsdValue = findViewById(R.id.balance_currency);
        arrayBalance = findViewById(R.id.balanceArray);
        text24Hours = findViewById(R.id.text_24_hrs);
        textAppreciation = findViewById(R.id.text_appreciation);
        issuer = findViewById(R.id.issuer);
        text24HoursSub = findViewById(R.id.text_24_hrs_sub);
        textAppreciationSub = findViewById(R.id.text_appreciation_sub);
        symbolText = findViewById(R.id.symbol);

        symbolText.setText(TextUtils.isEmpty(token.tokenInfo.name)
                ? token.tokenInfo.symbol.toUpperCase()
                : getString(R.string.token_name, token.tokenInfo.name, token.tokenInfo.symbol.toUpperCase()));

        TokenInfo tokenInfo = token.tokenInfo;
        BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, tokenInfo.decimals));
        BigDecimal ethBalance = tokenInfo.decimals > 0
                ? token.balance.divide(decimalDivisor) : token.balance;
        ethBalance = ethBalance.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        String value = ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "0" : ethBalance.toPlainString();
        balanceEth.setText(value);

        if (token.ticker == null) {
            textUsdValue.setText(R.string.NA);
            text24Hours.setText(R.string.NA);
            textAppreciation.setText(R.string.NA);
            textAppreciationSub.setText(R.string.appreciation);
            text24HoursSub.setText(R.string.twenty_four_hours);
        } else {
            // TODO: Fill token details
            textAppreciationSub.setText(R.string.appreciation);
            text24HoursSub.setText(R.string.twenty_four_hours);
        }

        balanceEth.setVisibility(View.VISIBLE);
        arrayBalance.setVisibility(View.GONE);
    }
}
