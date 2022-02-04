package com.alphawallet.app.ui;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.viewmodel.NameThisWalletViewModel;
import com.alphawallet.app.viewmodel.NewSettingsViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputView;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

@AndroidEntryPoint
public class NameThisWalletActivity extends BaseActivity implements StandardFunctionInterface {


    private NameThisWalletViewModel viewModel;

    private FunctionButtonBar functionBar;
    private InputView inputName;

    @Nullable
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name_this_wallet);

        toolbar();

        setTitle(getString(R.string.name_this_wallet));

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_save_name)));
        functionBar.revealButtons();

        inputName = findViewById(R.id.input_name);

        inputName.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    updateNameAndExit();
                    return true;
                }
                return false;
            }
        });

        viewModel = new ViewModelProvider(this)
                .get(NameThisWalletViewModel.class);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.ensName().observe(this, this::onENSSuccess);
    }

    public void onENSSuccess(String resolvedAddress) {
        inputName.getEditText().setHint(resolvedAddress);
    }

    private void onDefaultWallet(Wallet wallet) {
       inputName.setText(wallet.name);
    }

    public void handleClick(String action, int actionId) {
        updateNameAndExit();
    }

    private void updateNameAndExit() {
        viewModel.setWalletName(inputName.getText().toString(), this::finish);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (disposable != null && !disposable.isDisposed())
            disposable.dispose();

        disposable = null;
    }
}