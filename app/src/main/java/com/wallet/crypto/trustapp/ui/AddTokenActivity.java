package com.wallet.crypto.trustapp.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Address;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenInfo;
import com.wallet.crypto.trustapp.viewmodel.AddTokenViewModel;
import com.wallet.crypto.trustapp.viewmodel.AddTokenViewModelFactory;
import com.wallet.crypto.trustapp.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.trustapp.C.Key.WALLET;

public class AddTokenActivity extends BaseActivity implements View.OnClickListener {

    @Inject
    protected AddTokenViewModelFactory addTokenViewModelFactory;
    private AddTokenViewModel viewModel;

    private TextInputLayout addressLayout;
    private TextView address;
    private TextInputLayout symbolLayout;
    private TextView symbol;
    private TextInputLayout decimalsLayout;
    private TextView decimals;
    private TextView name;
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
        name = findViewById(R.id.textViewName);
        name.setEnabled(false);

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
            address.setText(token.address);
            symbol.setText(token.symbol);
            decimals.setText(String.valueOf(token.decimals));
            name.setText(token.name);
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

        if (!Address.isAddress(address)) {
            addressLayout.setError(getString(R.string.error_invalid_address));
            isValid = false;
        }

        if (isValid) {
            viewModel.save(address, symbol, decimals);
        }
    }
}
