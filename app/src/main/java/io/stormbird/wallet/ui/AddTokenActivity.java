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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Address;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.ui.zxing.FullScannerFragment;
import io.stormbird.wallet.util.QRURLParser;
import io.stormbird.wallet.viewmodel.AddTokenViewModel;
import io.stormbird.wallet.viewmodel.AddTokenViewModelFactory;
import io.stormbird.wallet.widget.InputAddressView;
import io.stormbird.wallet.widget.InputView;

public class AddTokenActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    protected AddTokenViewModelFactory addTokenViewModelFactory;
    private AddTokenViewModel viewModel;

    public LinearLayout ticketLayout;

    //Ticket Info
    public TextView venue;
    public TextView date;
    public TextView price;

    private Dialog dialog;
    private String lastCheck;

    LinearLayout progressLayout;

    public boolean isStormbird = false;

    public InputAddressView inputAddressView;
    public InputView symbolInputView;
    public InputView decimalsInputView;
    public InputView nameInputview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_token);

        toolbar();

        symbolInputView = findViewById(R.id.input_symbol);
        decimalsInputView = findViewById(R.id.input_decimal);
        nameInputview = findViewById(R.id.input_name);

        progressLayout = findViewById(R.id.layout_progress);

        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);

        ticketLayout = findViewById(R.id.layoutTicket);

        findViewById(R.id.save).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, addTokenViewModelFactory)
                .get(AddTokenViewModel.class);
        viewModel.progress().observe(this, this::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.result().observe(this, this::onSaved);
        viewModel.update().observe(this, this::onChecked);
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
                if (check.length() > 39 && check.length() < 43) {
                    if (!check.equals(lastCheck) && Address.isAddress(check)) {
                        //let's check the address here - see if we have an eth token
                        lastCheck = check; // don't get caught in a loop
                        onCheck(check);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        setTitle(R.string.empty);
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
                    String barcode = data.getParcelableExtra(FullScannerFragment.BarcodeObject);
                    if (barcode == null) barcode = data.getStringExtra(FullScannerFragment.BarcodeObject);

                    //if barcode is still null, ensure we don't GPF
                    if (barcode == null)
                    {
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    QRURLParser parser = QRURLParser.getInstance();
                    String extracted_address = parser.extractAddressFromQrString(barcode);
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

    private void onSaved(boolean result) {
        if (result) {
            viewModel.showTokens(this);
            finish();
        }
    }

    private void onChecked(boolean result) {
        if (result) {
            TokenInfo token = viewModel.tokenInfo().getValue();
            token.addTokenSetupPage(this);
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

    private void onCheck(String address) {
        viewModel.setupTokens(address);
    }

    private void onSave() {
        boolean isValid = true;
        String address = inputAddressView.getAddress();
        String symbol = symbolInputView.getText().toString().toLowerCase();
        String rawDecimals = decimalsInputView.getText().toString();
        String name = nameInputview.getText().toString();
//        String venue = this.venue.getText().toString();
//        String date = this.date.getText().toString();
//        String rawPrice = this.price.getText().toString();
//        double priceDb = 0;
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

//        try {
//            priceDb = Double.valueOf(rawPrice);
//        } catch (NumberFormatException ex) {
//            price.setError(getString(R.string.error_must_numeric));
//        }

        if (!Address.isAddress(address)) {
            inputAddressView.setError(getString(R.string.error_invalid_address));
            isValid = false;
        }

        if (isValid) {
            viewModel.save(address, symbol, decimals, name, isStormbird);
        }
    }
}
