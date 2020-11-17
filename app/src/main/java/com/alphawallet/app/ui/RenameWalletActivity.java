package com.alphawallet.app.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.viewmodel.WalletActionsViewModel;
import com.alphawallet.app.viewmodel.WalletActionsViewModelFactory;
import com.alphawallet.app.widget.InputAddress;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class RenameWalletActivity extends BaseActivity implements AddressReadyCallback
{
    @Inject
    WalletActionsViewModelFactory viewModelFactory;
    WalletActionsViewModel viewModel;

    private InputAddress inputAddress;
    private Wallet wallet;
    private boolean isNewWallet;
    private int walletCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename_wallet);
        toolbar();
        setTitle(getString(R.string.title_rename_wallet));

        initViewModel();

        if (getIntent() != null)
        {
            wallet = (Wallet) getIntent().getExtras().get("wallet");
            walletCount = getIntent().getIntExtra("walletCount", 0);
            walletCount++;
            isNewWallet = getIntent().getBooleanExtra("isNewWallet", false);
            initViews();
        }
        else
        {
            finish();
        }
    }

    public void initViewModel() {
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(WalletActionsViewModel.class);

        if (isNewWallet && wallet.name.isEmpty())
        {
            wallet.name = getString(R.string.wallet_name_template, walletCount);
            viewModel.storeWallet(wallet);
        }
    }

    public void initViews() {
        inputAddress = findViewById(R.id.input_ens);

        inputAddress.setAddress(wallet.name);
        inputAddress.setCursorAtLast();
        inputAddress.setAddressCallback(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        saveWalletName();

        if (inputAddress == null)
        {
            addressReady(null, null);
        }
        else
        {
            inputAddress.getAddress(false); //fetch the address and ENS name from the input view. Note - if ENS is still finishing this skips the waiting
        }

        finish();
    }

    private void saveWalletName() {
        wallet.name = inputAddress.getInputText();
        viewModel.storeWallet(wallet);
    }

    @Override
    public void addressReady(String address, String ensName)
    {
        //update ENS name if address matches and either there's no ENS name or it's a different ENS name
        //(user could have multiple ENS names and wants their wallet to be labelled with a different one)
        if (!TextUtils.isEmpty(address)
                && wallet.address.equalsIgnoreCase(address)
                && !TextUtils.isEmpty(ensName)
                && (TextUtils.isEmpty(wallet.name) || !ensName.equalsIgnoreCase(wallet.name))) //Wallet ENS currently empty or new ENS name is different
        {
            wallet.name = ensName;
            //update database
            viewModel.storeWallet(wallet);
        }
    }
}
