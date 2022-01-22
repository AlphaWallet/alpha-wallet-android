package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.SelectCurrencyActivity;
import com.alphawallet.app.ui.widget.entity.PriceAlert;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.alphawallet.app.C.EXTRA_CURRENCY;
import static com.alphawallet.app.C.EXTRA_STATE;

public class SetPriceAlertViewModel extends BaseViewModel {
    private final CurrencyRepositoryType currencyRepository;
    private final TokensService tokensService;

    SetPriceAlertViewModel(
            CurrencyRepositoryType currencyRepository,
            TokensService tokensService)
    {
        this.currencyRepository = currencyRepository;
        this.tokensService = tokensService;
    }

    public String getDefaultCurrency()
    {
        return currencyRepository.getDefaultCurrency();
    }

    public ArrayList<CurrencyItem> getCurrencyList()
    {
        return currencyRepository.getCurrencyList();
    }

    public void openCurrencySelection(Activity context, int requestCode, String currency)
    {
        Intent intent = new Intent(context, SelectCurrencyActivity.class);
        intent.putExtra(EXTRA_CURRENCY, currency);
        intent.putParcelableArrayListExtra(EXTRA_STATE, getCurrencyList());
        context.startActivityForResult(intent, requestCode);
    }

    public TokensService getTokensService() { return tokensService; }
}
