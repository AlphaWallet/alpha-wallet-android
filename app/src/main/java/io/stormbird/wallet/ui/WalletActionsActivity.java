package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Wallet;

public class WalletActionsActivity extends BaseActivity implements View.OnClickListener {
    private TextView walletBalance;
    private TextView walletAddress;
    private Button save;
    private EditText walletName;
    private TextView delete;
    private TextView backUp;

    private Wallet wallet;
    private String currencySymbol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_actions);
        toolbar();
        setTitle(R.string.empty);

        if (getIntent() != null) {
            wallet = (Wallet) getIntent().getExtras().get("wallet");
            currencySymbol = getIntent().getStringExtra("currency");
            initViews();
        } else {
            finish();
        }
    }

    private void initViews() {
        walletBalance = findViewById(R.id.wallet_balance);
        walletBalance.setText(String.format("%s %s", wallet.balance, currencySymbol));

        walletAddress = findViewById(R.id.wallet_address);
        walletAddress.setText(wallet.address);

        save = findViewById(R.id.button_save);
        save.setOnClickListener(this);

        walletName = findViewById(R.id.wallet_name);
        walletName.setText(wallet.name);
        walletName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                save.setEnabled(!wallet.name.equals(editable.toString()));
            }
        });

        delete = findViewById(R.id.delete);
        delete.setOnClickListener(this);

        backUp = findViewById(R.id.backup);
        backUp.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delete: {
                break;
            }
            case R.id.backup: {
                break;
            }
            case R.id.button_save: {
                break;
            }
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }
        return false;
    }
}
