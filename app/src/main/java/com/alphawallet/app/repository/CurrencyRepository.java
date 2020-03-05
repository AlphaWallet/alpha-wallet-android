package com.alphawallet.app.repository;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;

import java.util.ArrayList;
import java.util.Arrays;

public class CurrencyRepository implements CurrencyRepositoryType {
    public static final CurrencyItem[] CURRENCIES = {
            new CurrencyItem("USD", "American Dollar", "$", R.drawable.flag_usd),
            new CurrencyItem("EUR", "Euro", "€", R.drawable.flag_eur),
            new CurrencyItem("GBP", "British Pound", "£", R.drawable.flag_gbp),
            new CurrencyItem("AUD", "Australian Dollar", "$", R.drawable.flag_aud),
            new CurrencyItem("CNY", "China Yuan Renminbi","¥", R.drawable.flag_cny),
            new CurrencyItem("INR", "Indian Rupee","₹", R.drawable.flag_inr)
    };

    private final PreferenceRepositoryType preferences;

    public CurrencyRepository(PreferenceRepositoryType preferenceRepository) {
        this.preferences = preferenceRepository;
    }

    @Override
    public void setDefaultCurrency(String currencyCode) {
        preferences.setDefaultCurrency(currencyCode);
    }

    public String getDefaultCurrency() {
        return preferences.getDefaultCurrency();
    }

    @Override
    public ArrayList<CurrencyItem> getCurrencyList() {
        return new ArrayList<>(Arrays.asList(CURRENCIES));
    }

    public static CurrencyItem getCurrencyByISO(String currencyIsoCode) {
        for (CurrencyItem c : CURRENCIES) {
            if (currencyIsoCode.equals(c.getCode())) {
                return c;
            }
        }
        return null;
    }

    public static CurrencyItem getCurrencyByName(String currencyName) {
        for (CurrencyItem c : CURRENCIES) {
            if (currencyName.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }
}
