package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.entity.CurrencyItem;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
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
    private final PreferenceRepositoryType preferenceRepository;

    SetPriceAlertViewModel(
            CurrencyRepositoryType currencyRepository,
            PreferenceRepositoryType preferenceRepository)
    {
        this.currencyRepository = currencyRepository;
        this.preferenceRepository = preferenceRepository;
    }

    public String getDefaultCurrency()
    {
        return currencyRepository.getDefaultCurrency();
    }

    public ArrayList<CurrencyItem> getCurrencyList()
    {
        return currencyRepository.getCurrencyList();
    }

    public void openCurrencySelection(Activity context, int requestCode)
    {
        Intent intent = new Intent(context, SelectCurrencyActivity.class);
        String currentLocale = getDefaultCurrency();
        intent.putExtra(EXTRA_CURRENCY, currentLocale);
        intent.putParcelableArrayListExtra(EXTRA_STATE, getCurrencyList());
        context.startActivityForResult(intent, requestCode);
    }

    public void saveAlert(PriceAlert priceAlert)
    {
        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();

        ArrayList<PriceAlert> list;
        String json = preferenceRepository.getPriceAlerts();
        if (!json.isEmpty())
        {
            list = new Gson().fromJson(json, listType);
        }
        else
        {
            list = new ArrayList<>();
            list.add(priceAlert);
        }

        String updatedJson = new Gson().toJson(list, listType);

        preferenceRepository.setPriceAlerts(updatedJson);
    }

    public List<PriceAlert> fetchPriceAlerts()
    {
        Type listType = new TypeToken<List<PriceAlert>>() {}.getType();

        ArrayList<PriceAlert> list;
        String json = preferenceRepository.getPriceAlerts();
        if (!json.isEmpty())
        {
            list = new Gson().fromJson(json, listType);
        }
        else
        {
            list = new ArrayList<>();
        }
        return list;
    }
}
