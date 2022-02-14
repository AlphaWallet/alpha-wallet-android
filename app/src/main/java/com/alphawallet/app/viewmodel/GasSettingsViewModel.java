package com.alphawallet.app.viewmodel;


import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.TokensService;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.realm.Realm;

@HiltViewModel
public class GasSettingsViewModel extends BaseViewModel {
    private final TokensService tokensService;

    private final MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();
    private final MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();

    @Inject
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

    public Token getBaseCurrencyToken(long chainId)
    {
        return tokensService.getToken(chainId, tokensService.getCurrentAddress());
    }
}
