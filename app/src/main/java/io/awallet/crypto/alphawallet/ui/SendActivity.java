package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.ethereum.geth.Address;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.TokenTicker;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import io.awallet.crypto.alphawallet.util.BalanceUtils;
import io.awallet.crypto.alphawallet.util.QRURLParser;
import io.awallet.crypto.alphawallet.viewmodel.SendViewModel;
import io.awallet.crypto.alphawallet.viewmodel.SendViewModelFactory;
import io.awallet.crypto.alphawallet.widget.AWalletAlertDialog;

import static io.awallet.crypto.alphawallet.C.Key.WALLET;
import static io.awallet.crypto.alphawallet.entity.Token.EMPTY_BALANCE;

public class SendActivity extends BaseActivity {
    private static final float QR_IMAGE_WIDTH_RATIO = 0.9f;

    @Inject
    SendViewModelFactory sendViewModelFactory;
    SendViewModel viewModel;

    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private EditText toAddressText;
    private EditText amountText;

    // In case we're sending tokens
    private boolean sendingTokens = false;
    private String contractAddress;
    private int decimals;
    private String symbol;
    private Wallet wallet;
    private Token token;

    TextView titleConfirmTransfer;
    TextView toAddressError;
    TextView amountError;
    TextView amountConfirmText;

    TextInputLayout toInputLayout;
    TextInputLayout amountInputLayout;

    RelativeLayout inputAmountLayout;
    RelativeLayout transferOptionLayout;
    RelativeLayout confirmTransferLayout;

    FrameLayout sendSmsLayout;
    FrameLayout sendEmailLayout;
    FrameLayout inputAddressLayout;
    FrameLayout qrScannerLayout;

    Button nextButton;
    Button showAddressButton;
    Button transferButton;

    EditText amountEditText;
    EditText toAddressEditText;

    AWalletAlertDialog dialog;

    //Token
    public TextView balanceEth;
    public TextView balanceCurrency;
    public TextView symbolText;
    public ImageView icon;
    public TextView arrayBalance;
    public TextView text24Hours;
    public TextView textAppreciation;
    public TextView issuer;
    public TextView text24HoursSub;
    public TextView textAppreciationSub;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_send);
        toolbar();
        setTitle("");

        viewModel = ViewModelProviders.of(this, sendViewModelFactory)
                .get(SendViewModel.class);


        contractAddress = getIntent().getStringExtra(C.EXTRA_CONTRACT_ADDRESS);
        decimals = getIntent().getIntExtra(C.EXTRA_DECIMALS, C.ETHER_DECIMALS);
        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? C.ETH_SYMBOL : symbol;
        sendingTokens = getIntent().getBooleanExtra(C.EXTRA_SENDING_TOKENS, false);
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);

        setupContent();

        initViews();

        // Populate to address if it has been passed forward
        String toAddress = getIntent().getStringExtra(C.EXTRA_ADDRESS);
        if (toAddress != null) {
            toAddressText.setText(toAddress);
        }
    }

    private void initViews() {
        titleConfirmTransfer = findViewById(R.id.title_confirm_transfer);
        toAddressError = findViewById(R.id.to_address_error);
        amountError = findViewById(R.id.amount_error);

        toInputLayout = findViewById(R.id.to_input_layout);
        toAddressText = findViewById(R.id.send_to_address);
        amountInputLayout = findViewById(R.id.amount_input_layout);
        amountText = findViewById(R.id.send_amount);
        amountInputLayout.setHint(getString(R.string.hint_amount) + " " + symbol);

//        ImageButton scanBarcodeButton = findViewById(R.id.scan_barcode_button);
//        scanBarcodeButton.setOnClickListener(view -> {
//            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
//            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
//        });

        ImageView qrImageView = findViewById(R.id.qr_image);
        final Bitmap qrCode = createQRImage(contractAddress);
        qrImageView.setImageBitmap(qrCode);
        ((TextView) findViewById(R.id.address)).setText(contractAddress);

        inputAmountLayout = findViewById(R.id.layout_input_amount);
        transferOptionLayout = findViewById(R.id.layout_transfer_option);
        confirmTransferLayout = findViewById(R.id.layout_confirm_transfer);
        toAddressEditText = findViewById(R.id.edit_to_address);
        amountEditText = findViewById(R.id.edit_amount);
        amountConfirmText = findViewById(R.id.text_amount_confirm);

        qrScannerLayout = findViewById(R.id.layout_qr_scanner);
        qrScannerLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });
        inputAddressLayout = findViewById(R.id.layout_input_address);
        inputAddressLayout.setOnClickListener(v -> {
            toAddressEditText.getText().clear();
            onConfirmTransfer();
        });
        nextButton = findViewById(R.id.button_next);
        nextButton.setOnClickListener(v -> {
            onNext();
        });
        showAddressButton = findViewById(R.id.button_show_address);
        showAddressButton.setOnClickListener(v -> {
            viewModel.showMyAddress(this, wallet);
        });
        transferButton = findViewById(R.id.button_transfer);
        transferButton.setOnClickListener(v -> {
            onTransfer();
        });
    }

    private void onNext() {
        dismissKeyboard();
        amountError.setVisibility(View.GONE);
        final String amount = amountEditText.getText().toString();
        if (!isValidAmount(amount)) {
            amountError.setVisibility(View.VISIBLE);
            amountError.setText(R.string.error_invalid_amount);
            return;
        } else {
            String amountText = amountEditText.getText().toString() + " " + symbol;
            amountConfirmText.setText(amountText);
            inputAmountLayout.setVisibility(View.GONE);
            transferOptionLayout.setVisibility(View.VISIBLE);
            confirmTransferLayout.setVisibility(View.GONE);
        }
    }

    private void onConfirmTransfer() {
        inputAmountLayout.setVisibility(View.GONE);
        transferOptionLayout.setVisibility(View.GONE);
        confirmTransferLayout.setVisibility(View.VISIBLE);
    }

    private void onBack() {
        if (inputAmountLayout.getVisibility() == View.VISIBLE) {
            finish();
        } else if (confirmTransferLayout.getVisibility() == View.VISIBLE) {
            toAddressEditText.getText().clear();
            inputAmountLayout.setVisibility(View.GONE);
            transferOptionLayout.setVisibility(View.VISIBLE);
            confirmTransferLayout.setVisibility(View.GONE);
        } else {
            inputAmountLayout.setVisibility(View.VISIBLE);
            transferOptionLayout.setVisibility(View.GONE);
            confirmTransferLayout.setVisibility(View.GONE);
        }
    }

    private Bitmap createQRImage(String address) {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int imageSize = (int) (size.x * QR_IMAGE_WIDTH_RATIO);
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    address,
                    BarcodeFormat.QR_CODE,
                    imageSize,
                    imageSize,
                    null);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_fail_generate_qr), Toast.LENGTH_SHORT)
                    .show();
        }
        return null;
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
                    Point[] p = barcode.cornerPoints;
                    toAddressEditText.setText(extracted_address);
                    onTransfer();
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

    private void onTransfer() {
        toAddressError.setVisibility(View.GONE);
        final String to = toAddressEditText.getText().toString();
        if (!isAddressValid(to)) {
            toAddressError.setVisibility(View.VISIBLE);
            toAddressError.setText(getString(R.string.error_invalid_address));
            return;
        }

        BigInteger amountInSubunits = BalanceUtils.baseToSubunit(amountEditText.getText().toString(), decimals);
        viewModel.openConfirmation(this, to, amountInSubunits, contractAddress, decimals, symbol, sendingTokens);
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

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    public void setupContent() {
        icon = findViewById(R.id.icon);
        balanceEth = findViewById(R.id.balance_eth);
        balanceCurrency = findViewById(R.id.balance_currency);
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
            balanceCurrency.setText(EMPTY_BALANCE);
            text24Hours.setText(EMPTY_BALANCE);
            textAppreciation.setText(EMPTY_BALANCE);
            textAppreciationSub.setText(R.string.appreciation);
            text24HoursSub.setText(R.string.twenty_four_hours);
        } else {
            fillCurrency(ethBalance, token.ticker);
            textAppreciationSub.setText(R.string.appreciation);
            text24HoursSub.setText(R.string.twenty_four_hours);
        }

        balanceEth.setVisibility(View.VISIBLE);
        arrayBalance.setVisibility(View.GONE);
    }

    public void fillCurrency(BigDecimal ethBalance, TokenTicker ticker) {
        String converted = ethBalance.compareTo(BigDecimal.ZERO) == 0
                ? EMPTY_BALANCE
                : ethBalance.multiply(new BigDecimal(ticker.price))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        String formattedPercents = "";
        int color = Color.RED;
        double percentage = 0;
        try {
            percentage = Double.valueOf(ticker.percentChange24h);
            color = ContextCompat.getColor(this, percentage < 0 ? R.color.red : R.color.green);
            formattedPercents = (percentage < 0 ? "" : "+") + ticker.percentChange24h + "%";
            text24Hours.setText(formattedPercents);
            text24Hours.setTextColor(color);
        } catch (Exception ex) { /* Quietly */ }
        String lbl = getString(R.string.token_balance,
                ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "" : "$",
                converted);
        Spannable spannable = new SpannableString(lbl);
        spannable.setSpan(new ForegroundColorSpan(color),
                converted.length() + 1, lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.balanceCurrency.setText(spannable);

        //calculate the appreciation value
        double dBalance = ethBalance.multiply(new BigDecimal(ticker.price)).doubleValue();
        double nPercentage = (100.0 + percentage) / 100.0;
        double dAppreciation = dBalance - (dBalance / nPercentage);
        BigDecimal appreciation = BigDecimal.valueOf(dAppreciation);

        int valColor;
        if (appreciation.compareTo(BigDecimal.ZERO) == 1) {
            valColor = ContextCompat.getColor(this, R.color.black);
            textAppreciationSub.setText(R.string.appreciation);
            textAppreciationSub.setTextColor(valColor);
        } else {
            valColor = ContextCompat.getColor(this, R.color.red);
            textAppreciationSub.setText(R.string.depreciation);
            textAppreciationSub.setTextColor(valColor);
            appreciation = appreciation.multiply(BigDecimal.valueOf(-1));
        }

        //BigDecimal appreciation = balance.subtract(balance.divide((BigDecimal.valueOf(percentage).add(BigDecimal.ONE))) );
        String convertedAppreciation =
                appreciation
                        .setScale(2, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString();

        lbl = getString(R.string.token_balance,
                ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "" : "$",
                convertedAppreciation);

        spannable = new SpannableString(lbl);
        spannable.setSpan(new ForegroundColorSpan(color),
                convertedAppreciation.length() + 1, lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.textAppreciation.setText(spannable);


    }
}
