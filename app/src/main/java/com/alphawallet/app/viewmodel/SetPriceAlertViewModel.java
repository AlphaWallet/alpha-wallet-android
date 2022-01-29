package com.alphawallet.app.viewmodel;

import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.service.TokensService;

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

    public TokensService getTokensService() { return tokensService; }
}
