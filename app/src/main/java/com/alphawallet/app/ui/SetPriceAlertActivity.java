package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.entity.InputFiatCallback;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.alphawallet.app.viewmodel.SetPriceAlertViewModel;
import com.alphawallet.app.viewmodel.SetPriceAlertViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputFiatView;
import com.alphawallet.ethereum.EthereumNetworkBase;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SetPriceAlertActivity extends BaseActivity implements InputFiatCallback {
    private static final int REQUEST_SELECT_CURRENCY = 3000;

    @Inject
    SetPriceAlertViewModelFactory viewModelFactory;

    private InputFiatView inputView;
    private FunctionButtonBar functionBar;
    private SetPriceAlertViewModel viewModel;
    private PriceAlert newPriceAlert;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        if (getIntent() != null)
        {
            setContentView(R.layout.activity_set_price_alert);
            toolbar();
            setTitle(getString(R.string.title_set_new_alert));

            inputView = findViewById(R.id.input_view);
            inputView.setCallback(this);

            functionBar = findViewById(R.id.button_bar);
            functionBar.setPrimaryButtonText(R.string.action_save_alert);
            functionBar.setPrimaryButtonClickListener(v -> saveAlert());
            functionBar.setPrimaryButtonEnabled(false);

            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(SetPriceAlertViewModel.class);

            int chainId = getIntent().getIntExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            Token token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));

            newPriceAlert = new PriceAlert(viewModel.getDefaultCurrency(), token.tokenInfo.name);

            inputView.showKeyboard();
        }
    }

    private void saveAlert()
    {
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_PRICE_ALERT, newPriceAlert);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onInputChanged(String s)
    {
        if (!s.isEmpty())
        {
            double value = Double.parseDouble(s);
            if (value > 0)
            {
                functionBar.setPrimaryButtonEnabled(true);
                newPriceAlert.setValue(String.valueOf(value));
            }
            else
            {
                functionBar.setPrimaryButtonEnabled(false);
            }
        }
    }

    @Override
    public void onMoreClicked()
    {
        viewModel.openCurrencySelection(this, REQUEST_SELECT_CURRENCY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (requestCode == REQUEST_SELECT_CURRENCY)
        {
            if (resultCode == RESULT_OK)
            {
                if (data != null)
                {
                    String currency = data.getStringExtra(C.EXTRA_CURRENCY);
                    inputView.setCurrency(currency);
                    newPriceAlert.setCurrency(currency);
                }
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
