package com.wallet.crypto.alphawallet.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;

import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Address;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;

import com.wallet.crypto.alphawallet.entity.TokenInfo;
import com.wallet.crypto.alphawallet.ui.barcode.BarcodeCaptureActivity;
import com.wallet.crypto.alphawallet.util.QRURLParser;
import com.wallet.crypto.alphawallet.viewmodel.AddTokenViewModel;
import com.wallet.crypto.alphawallet.viewmodel.AddTokenViewModelFactory;
import com.wallet.crypto.alphawallet.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class AddTokenActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    protected AddTokenViewModelFactory addTokenViewModelFactory;
    private AddTokenViewModel viewModel;

    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private TextInputLayout addressLayout;
    public TextView address;
    private TextInputLayout symbolLayout;
    public TextView symbol;
    private TextInputLayout decimalsLayout;
    public TextView decimals;
    public TextView name;
    public LinearLayout ticketLayout;

    //Ticket Info
    public TextView venue;
    public TextView date;
    public TextView price;

    private SystemView systemView;
    private Dialog dialog;
    private String lastCheck;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_token);

        toolbar();

        addressLayout = findViewById(R.id.address_input_layout);
        address = findViewById(R.id.address);
        symbolLayout = findViewById(R.id.symbol_input_layout);
        symbol = findViewById(R.id.symbol);
        decimalsLayout = findViewById(R.id.decimal_input_layout);
        decimals = findViewById(R.id.decimals);
        systemView = findViewById(R.id.system_view);
        systemView.hide();

        venue = findViewById(R.id.textViewVenue);
        date = findViewById(R.id.textViewDate);
        price = findViewById(R.id.textViewPrice);
        name = findViewById(R.id.textViewName);
        ticketLayout = findViewById(R.id.layoutTicket);

        findViewById(R.id.save).setOnClickListener(this);

        viewModel = ViewModelProviders.of(this, addTokenViewModelFactory)
                .get(AddTokenViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.result().observe(this, this::onSaved);
        viewModel.update().observe(this, this::onChecked);
        lastCheck = "";

        address.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //wait until we have an ethereum address
                String check = address.getText().toString().toLowerCase();
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

        ImageButton scanBarcodeButton = findViewById(R.id.scan_contract_address_qr);
        scanBarcodeButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
            startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
        });
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
                        Toast.makeText(this, R.string.toast_qr_code_no_address, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Point[] p = barcode.cornerPoints;
                    address.setText(extracted_address);
                }
            } else {
                Log.e("SEND", String.format(getString(R.string.barcode_error_format),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
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
        String address = this.address.getText().toString();
        String symbol = this.symbol.getText().toString().toLowerCase();
        String rawDecimals = this.decimals.getText().toString();
        String name = this.name.getText().toString();
        String venue = this.venue.getText().toString();
        String date = this.date.getText().toString();
        String rawPrice = this.price.getText().toString();

        double priceDb = 0;
        int decimals = 0;

        if (TextUtils.isEmpty(address)) {
            addressLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(symbol)) {
            symbolLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(rawDecimals)) {
            decimalsLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        try {
            decimals = Integer.valueOf(rawDecimals);
        } catch (NumberFormatException ex) {
            decimalsLayout.setError(getString(R.string.error_must_numeric));
            isValid = false;
        }

        try {
            priceDb = Double.valueOf(rawPrice);
        } catch (NumberFormatException ex) {
            price.setError(getString(R.string.error_must_numeric));
        }

        if (!Address.isAddress(address)) {
            addressLayout.setError(getString(R.string.error_invalid_address));
            isValid = false;
        }

        if (isValid) {
            viewModel.save(address, symbol, decimals, name, venue, date, priceDb);
        }
    }
}
