package com.alphawallet.app.viewmodel;

import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.service.TokensService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SetPriceAlertViewModel extends BaseViewModel {
    private final CurrencyRepositoryType currencyRepository;
    private final TokensService tokensService;

    @Inject
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

    public TokensService getTokensService() { return tokensService; }
}
