package com.alphawallet.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;

import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.viewmodel.CustomNetworkViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

import static java.util.Collections.singletonList;

@AndroidEntryPoint
public class AddCustomRPCNetworkActivity extends BaseActivity implements StandardFunctionInterface
{

    public static final String CHAIN_ID = "chain_id";

    private CustomNetworkViewModel viewModel;

    private InputView nameInputView;
    private InputView rpcUrlInputView;
    private InputView chainIdInputView;
    private InputView symbolInputView;
    private InputView blockExplorerUrlInputView;
    private InputView blockExplorerApiUrl;
    private MaterialCheckBox testNetCheckBox;

    private final Handler handler = new Handler();
    private long chainId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_custom_rpc_network);

        toolbar();
        setTitle(getString(R.string.title_activity_add_custom_rpcnetwork));

        nameInputView = findViewById(R.id.input_network_name);
        nameInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        nameInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        rpcUrlInputView = findViewById(R.id.input_network_rpc_url);
        rpcUrlInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        rpcUrlInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        chainIdInputView = findViewById(R.id.input_network_chain_id);
        chainIdInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        chainIdInputView.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        symbolInputView = findViewById(R.id.input_network_symbol);
        symbolInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        symbolInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        blockExplorerUrlInputView = findViewById(R.id.input_network_block_explorer_url);
        blockExplorerUrlInputView.getEditText().setImeOptions(EditorInfo.IME_ACTION_NEXT);
        blockExplorerUrlInputView.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        blockExplorerUrlInputView.getEditText().setHint("https://etherscan.com/tx/");

        blockExplorerApiUrl = findViewById(R.id.input_network_explorer_api);
        blockExplorerApiUrl.getEditText().setImeOptions(EditorInfo.IME_ACTION_DONE);
        blockExplorerApiUrl.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        blockExplorerApiUrl.getEditText().setHint("https://api.etherscan.io/api?");

        testNetCheckBox = findViewById(R.id.checkbox_testnet);

        initViewModel();

        chainId = getIntent().getLongExtra(CHAIN_ID, -1);
        boolean isEditMode = chainId >= 0;

        if (isEditMode)
        {
            setTitle(getString(R.string.title_network_info));
            // get network info and fill ui
            NetworkInfo network = viewModel.getNetworkInfo(chainId);

            renderNetwork(network);

            List<Integer> buttons = new ArrayList<>();
            buttons.add(R.string.action_update_network);

            if (!network.isCustom)
            {
                chainIdInputView.getEditText().setEnabled(false);
                buttons.add(R.string.action_reset_network);
            }
            addFunctionBar(buttons);
        }
        else
        {
            addFunctionBar(singletonList(R.string.action_add_network));
        }
    }

    private void addFunctionBar(List<Integer> functionResources)
    {
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, functionResources);
        functionBar.revealButtons();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(CustomNetworkViewModel.class);
    }

    private boolean validateInputs()
    {
        if (TextUtils.isEmpty(nameInputView.getText()))
        {
            nameInputView.setError(getString(R.string.error_field_required));
            return false;
        }

        if (TextUtils.isEmpty(rpcUrlInputView.getText()))
        {
            rpcUrlInputView.setError(getString(R.string.error_field_required));
            return false;
        }
        else if (!URLUtil.isValidUrl(rpcUrlInputView.getText().toString()))
        {
            rpcUrlInputView.setError(getString(R.string.error_invalid_url));
            return false;
        }

        if (TextUtils.isEmpty(chainIdInputView.getText()))
        {
            chainIdInputView.setError(getString(R.string.error_field_required));
            return false;
        }
        else
        {
            try
            {
                Long.parseLong(chainIdInputView.getText().toString());
            }
            catch (NumberFormatException ex)
            {
                chainIdInputView.setError(getString(R.string.error_must_numeric));
                return false;
            }
        }

        long newChainId = Long.parseLong(chainIdInputView.getText().toString());
        long chainId = getIntent().getLongExtra(CHAIN_ID, -1);
        if (newChainId != chainId)
        {
            NetworkInfo network = viewModel.getNetworkInfo(newChainId);
            if (network != null)
            {
                chainIdInputView.setError(getString(R.string.error_chainid_already_taken));
                return false;
            }
        }

        if (TextUtils.isEmpty(symbolInputView.getText()))
        {
            symbolInputView.setError(getString(R.string.error_field_required));
            return false;
        }

        //Allow blank for these
        /*if (TextUtils.isEmpty(blockExplorerUrlInputView.getText())) {
            blockExplorerUrlInputView.setError(getString(R.string.error_field_required));
            return false;
        } else*/
        if (!TextUtils.isEmpty(blockExplorerUrlInputView.getText().toString()) && !URLUtil.isValidUrl(blockExplorerUrlInputView.getText().toString()))
        {
            blockExplorerUrlInputView.setError(getString(R.string.error_invalid_url));
            return false;
        }

        /*if (TextUtils.isEmpty(blockExplorerApiUrl.getText())) {
            blockExplorerApiUrl.setError(getString(R.string.error_field_required));
            return false;
        } else*/
        if (!TextUtils.isEmpty(blockExplorerApiUrl.getText().toString()) && !URLUtil.isValidUrl(blockExplorerApiUrl.getText().toString()))
        {
            blockExplorerApiUrl.setError(getString(R.string.error_invalid_url));
            return false;
        }

        return true;
    }

    private void resetValidateErrors()
    {
        nameInputView.setError(null);
        rpcUrlInputView.setError(null);
        chainIdInputView.setError(null);
        symbolInputView.setError(null);
        blockExplorerUrlInputView.setError(null);
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (actionId == R.string.action_reset_network)
        {
            resetDefault();
            return;
        }

        if (validateInputs())
        {
            viewModel.saveNetwork(nameInputView.getText().toString(),
                    rpcUrlInputView.getText().toString(),
                    Long.parseLong(chainIdInputView.getText().toString()),
                    symbolInputView.getText().toString(),
                    blockExplorerUrlInputView.getText().toString(),
                    blockExplorerApiUrl.getText().toString(), testNetCheckBox.isChecked(), chainId != -1L ? chainId : null);
            finish();
        }
        else
        {
            handler.postDelayed(this::resetValidateErrors, 2000);
        }
    }

    private void resetDefault()
    {
        NetworkInfo network = viewModel.getBuiltInNetwork(chainId);
        renderNetwork(network);
    }

    private void renderNetwork(NetworkInfo network)
    {
        nameInputView.setText(network.name);
        rpcUrlInputView.setText(network.rpcServerUrl.replaceAll("(/)([0-9a-fA-F]{32})", "/********************************"));
        chainIdInputView.setText(String.valueOf(network.chainId));
        symbolInputView.setText(network.symbol);
        blockExplorerUrlInputView.setText(network.etherscanUrl);
        blockExplorerApiUrl.setText(network.etherscanAPI);
        testNetCheckBox.setChecked(viewModel.isTestNetwork(network));
    }
}