package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.ui.widget.entity.InputFiatCallback;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.alphawallet.app.viewmodel.SetPriceAlertViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.InputFiatView;
import com.alphawallet.ethereum.EthereumNetworkBase;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@AndroidEntryPoint
public class SetPriceAlertActivity extends BaseActivity implements InputFiatCallback {
    private static final int REQUEST_SELECT_CURRENCY = 3000;

    @Inject
    TickerService tickerService;

    private InputFiatView inputView;
    private FunctionButtonBar functionBar;
    private SetPriceAlertViewModel viewModel;
    private PriceAlert newPriceAlert;
    private Token token;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {

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

            viewModel = new ViewModelProvider(this)
                    .get(SetPriceAlertViewModel.class);

            long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));

            newPriceAlert = new PriceAlert(viewModel.getDefaultCurrency(), token.tokenInfo.name, token.tokenInfo.address, token.tokenInfo.chainId);

            inputView.showKeyboard();
            updateTokenPrice();
        }
    }

    private void updateTokenPrice() {
        tickerService.convertPair(TickerService.getCurrencySymbolTxt(), newPriceAlert.getCurrency())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((rate) -> {
                    TokenTicker tokenTicker = viewModel.getTokensService().getTokenTicker(token);
                    if (tokenTicker == null) {
                        return;
                    }
                    double currentTokenPrice = Double.parseDouble(tokenTicker.price);
                    CurrencyItem currencyItem = CurrencyRepository.getCurrencyByISO(newPriceAlert.getCurrency());
                    if (currencyItem != null) {
                        String text = currencyItem.getCurrencyText(currentTokenPrice * rate);
                        inputView.setSubTextValue(text);
                    }
                }, Throwable::printStackTrace).isDisposed();
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
        if (s.isEmpty()) {
            functionBar.setPrimaryButtonEnabled(false);
            return;
        }

        try {
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
        } catch (NumberFormatException nfe) {
            functionBar.setPrimaryButtonEnabled(false);
        }
    }

    @Override
    public void onMoreClicked()
    {
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
                    updateTokenPrice();
                }
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
