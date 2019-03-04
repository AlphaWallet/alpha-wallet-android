package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.viewmodel.WalletActionsViewModel;
import io.stormbird.wallet.viewmodel.WalletActionsViewModelFactory;

public class WalletActionsActivity extends BaseActivity implements View.OnClickListener {
    @Inject
    WalletActionsViewModelFactory walletActionsViewModelFactory;
    WalletActionsViewModel viewModel;

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
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_actions);
        toolbar();
        setTitle(R.string.empty);

        initViewModel();

        if (getIntent() != null) {
            wallet = (Wallet) getIntent().getExtras().get("wallet");
            currencySymbol = getIntent().getStringExtra("currency");
            initViews();
        } else {
            finish();
        }
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, walletActionsViewModelFactory)
                .get(WalletActionsViewModel.class);
    }

    private void initViews() {
        walletBalance = findViewById(R.id.wallet_balance);
        walletBalance.setText(String.format("%s %s", wallet.balance, currencySymbol));

        walletAddress = findViewById(R.id.wallet_address);
        walletAddress.setText(wallet.address);

        save = findViewById(R.id.button_save);
        save.setOnClickListener(this);

        TextWatcher walletNameWatcher = new TextWatcher() {
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
        };

        walletName = findViewById(R.id.wallet_name);
        walletName.setText(wallet.name);
        walletName.addTextChangedListener(walletNameWatcher);

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
                wallet.name = walletName.getText().toString();
                viewModel.storeWallet(wallet);
                save.setEnabled(false);
                showWalletsActivity();
                finish();
                break;
            }
        }
    }

    private void showWalletsActivity() {
        Intent intent = new Intent(this, WalletsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("shouldRefresh", true);
        startActivity(intent);
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
