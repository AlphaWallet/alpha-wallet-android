package com.wallet.crypto.trustapp.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;

import java.math.BigInteger;

public class GasSettingsViewModel extends BaseViewModel {

    public static final int SET_GAS_SETTINGS = 1;

    private FindDefaultNetworkInteract findDefaultNetworkInteract;

    private MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();
    private MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();
    private MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    public GasSettingsViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        gasPrice.setValue(new BigInteger("0"));
        gasLimit.setValue(new BigInteger("0"));
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

    public BigInteger networkFee() {
        return gasPrice.getValue().multiply(gasLimit.getValue());
    }

}
