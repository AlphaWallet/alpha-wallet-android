package com.alphawallet.app.viewmodel;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.service.TokensService;

import java.math.BigDecimal;
import java.math.BigInteger;

import io.realm.Realm;

public class GasSettingsViewModel extends BaseViewModel {

    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final TokensService tokensService;

    private MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();
    private MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();
    private MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    public GasSettingsViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract, TokensService svs) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.tokensService = svs;
        gasPrice.setValue(BigInteger.ZERO);
        gasLimit.setValue(BigInteger.ZERO);
    }

    public void prepare() {
        findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
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

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.setValue(networkInfo);
    }

    public BigDecimal networkFee() {
        return new BigDecimal(gasPrice.getValue().multiply(gasLimit.getValue()));
    }

    public Token getBaseCurrencyToken(int chainId)
    {
        return tokensService.getToken(chainId, tokensService.getCurrentAddress());
    }
}
