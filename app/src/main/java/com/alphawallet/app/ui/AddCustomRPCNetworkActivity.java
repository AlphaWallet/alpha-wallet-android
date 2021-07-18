package com.alphawallet.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.viewmodel.CustomNetworkViewModel;
import com.alphawallet.app.viewmodel.CustomNetworkViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class AddCustomRPCNetworkActivity extends BaseActivity implements StandardFunctionInterface {

    @Inject
    CustomNetworkViewModelFactory walletViewModelFactory;
    private CustomNetworkViewModel viewModel;

    private InputView nameInputView;
    private InputView rpcUrlInputView;
    private InputView chainIdInputView;
    private InputView symbolInputView;
    private InputView blockExplorerUrlInputView;
    private SwitchMaterial testNetSwitch;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_custom_rpc_network);
        AndroidInjection.inject(this);

        toolbar();

        nameInputView = findViewById(R.id.input_network_name);
        rpcUrlInputView = findViewById(R.id.input_network_rpc_url);
        chainIdInputView = findViewById(R.id.input_network_chain_id);
        symbolInputView = findViewById(R.id.input_network_symbol);
        blockExplorerUrlInputView = findViewById(R.id.input_network_block_explorer_url);
        testNetSwitch = findViewById(R.id.testnet_switch);

        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_add_network)));
        functionBar.revealButtons();

        initViewModel();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this, walletViewModelFactory)
                .get(CustomNetworkViewModel.class);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(nameInputView.getText())) {
            nameInputView.setError(getString(R.string.error_field_required));
            return false;
        }

        if (TextUtils.isEmpty(rpcUrlInputView.getText())) {
            rpcUrlInputView.setError(getString(R.string.error_field_required));
            return false;
        } else if (!URLUtil.isValidUrl(rpcUrlInputView.getText().toString())) {
            rpcUrlInputView.setError(getString(R.string.error_invalid_url));
            return false;
        }

        if (TextUtils.isEmpty(chainIdInputView.getText())) {
            chainIdInputView.setError(getString(R.string.error_field_required));
            return false;
        } else {
            try {
                Integer.parseInt(chainIdInputView.getText().toString());
            } catch (NumberFormatException ex) {
                chainIdInputView.setError(getString(R.string.error_must_numeric));
                return false;
            }
        }

        if (TextUtils.isEmpty(symbolInputView.getText())) {
            symbolInputView.setError(getString(R.string.error_field_required));
            return false;
        }

        if (TextUtils.isEmpty(blockExplorerUrlInputView.getText())) {
            blockExplorerUrlInputView.setError(getString(R.string.error_field_required));
            return false;
        } else if (!URLUtil.isValidUrl(blockExplorerUrlInputView.getText().toString())) {
            blockExplorerUrlInputView.setError(getString(R.string.error_invalid_url));
            return false;
        }

        return true;
    }

    private void resetValidateErrors() {
        nameInputView.setError(null);
        rpcUrlInputView.setError(null);
        chainIdInputView.setError(null);
        symbolInputView.setError(null);
        blockExplorerUrlInputView.setError(null);
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (validateInputs()) {
            // add network
            viewModel.addNetwork(nameInputView.getText().toString(),
                    rpcUrlInputView.getText().toString(),
                    Integer.parseInt(chainIdInputView.getText().toString()),
                    symbolInputView.getText().toString(),
                    blockExplorerUrlInputView.getText().toString(), testNetSwitch.isChecked());
            finish();
        } else {
            handler.postDelayed(this::resetValidateErrors, 2000);
        }
    }
}