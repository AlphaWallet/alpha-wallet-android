package com.alphawallet.app.viewmodel;


import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.TokensService;

import java.math.BigDecimal;
import java.math.BigInteger;

import io.realm.Realm;

public class GasSettingsViewModel extends BaseViewModel {
    private final TokensService tokensService;

    private MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();
    private MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();

    public GasSettingsViewModel(TokensService svs) {
        this.tokensService = svs;
        gasPrice.setValue(BigInteger.ZERO);
        gasLimit.setValue(BigInteger.ZERO);
    }

    public Realm getTickerRealm()
    {
        return tokensService.getTickerRealmInstance();
    }

    public MutableLiveData<BigInteger> gasPrice() {
        return gasPrice;
    }

    public MutableLiveData<BigInteger> gasLimit() {
        return gasLimit;
    }

    public BigDecimal networkFee() {
        return new BigDecimal(gasPrice.getValue().multiply(gasLimit.getValue()));
    }

    public Token getBaseCurrencyToken(int chainId)
    {
        return tokensService.getToken(chainId, tokensService.getCurrentAddress());
    }
}
