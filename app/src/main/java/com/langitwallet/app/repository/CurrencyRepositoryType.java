package com.langitwallet.app.repository;

import com.langitwallet.app.entity.CurrencyItem;

import java.util.ArrayList;

public interface CurrencyRepositoryType {
    String getDefaultCurrency();

    void setDefaultCurrency(String currency);

    ArrayList<CurrencyItem> getCurrencyList();
}
