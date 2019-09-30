package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;

import java.math.BigDecimal;
import java.math.BigInteger;

public class GasSettingsViewModel extends BaseViewModel {

    public static final int SET_GAS_SETTINGS = 1;

    private FindDefaultNetworkInteract findDefaultNetworkInteract;

    private MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();
    private MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();
    private MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    public GasSettingsViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        gasPrice.setValue(BigInteger.ZERO);
        gasLimit.setValue(BigInteger.ZERO);
    }

    public void prepare() {
        findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
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

}
