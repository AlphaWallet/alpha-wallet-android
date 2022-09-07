package com.alphawallet.app.repository;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.CurrencyItem;

import java.util.ArrayList;
import java.util.Arrays;

public class CurrencyRepository implements CurrencyRepositoryType {
    /**
     * Find currency symbol here: https://coinyep.com/en/currencies
     * Find drawables here - https://github.com/Shusshu/android-flags/tree/master/flags/src/main/res/drawable
     */
    public static final CurrencyItem[] CURRENCIES = {
            new CurrencyItem("USD", "American Dollar", "$", R.drawable.ic_flags_usa),
            new CurrencyItem("EUR", "Euro", "€", R.drawable.ic_flags_euro),
            new CurrencyItem("GBP", "British Pound", "£", R.drawable.ic_flags_uk),
            new CurrencyItem("AUD", "Australian Dollar", "$", R.drawable.ic_flags_australia),
            new CurrencyItem("CNY", "China Yuan Renminbi","¥", R.drawable.ic_flags_china),
            new CurrencyItem("INR", "Indian Rupee","₹", R.drawable.ic_flags_india),
            new CurrencyItem("SGD", "Singapore Dollar","$", R.drawable.ic_flag_sgd),
            new CurrencyItem("JPY", "Japanese Yen","¥", R.drawable.ic_flags_japan),
            new CurrencyItem("KRW", "Korean Won","₩", R.drawable.ic_flags_korea),
            new CurrencyItem("RUB", "Russian Ruble","₽", R.drawable.ic_flags_russia),
            new CurrencyItem("VND", "Vietnamese đồng", "₫", R.drawable.ic_flags_vietnam),
            new CurrencyItem("PKR", "Pakistani Rupee", "Rs", R.drawable.ic_flags_pakistan),
            new CurrencyItem("MMK", "Myanmar Kyat", "Ks", R.drawable.ic_flags_myanmar),
            new CurrencyItem("IDR", "Indonesian Rupiah", "Rp", R.drawable.ic_flags_indonesia)
    };

    private final PreferenceRepositoryType preferences;

    public CurrencyRepository(PreferenceRepositoryType preferenceRepository) {
        this.preferences = preferenceRepository;
    }

    @Override
    public void setDefaultCurrency(String currencyCode) {
        CurrencyItem currencyItem = getCurrencyByISO(currencyCode);
        preferences.setDefaultCurrency(currencyItem);
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

    public static int getFlagByISO(String currencyIsoCode) {
        for (CurrencyItem c : CURRENCIES) {
            if (currencyIsoCode.equals(c.getCode())) {
                return c.getFlag();
            }
        }
        return 0;
    }
}
