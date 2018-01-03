package com.wallet.crypto.trustapp.viewmodel;


import android.arch.lifecycle.MutableLiveData;

import java.math.BigInteger;

public class GasSettingsViewModel extends BaseViewModel {

    public static final int SET_GAS_SETTINGS = 1;
    private MutableLiveData<BigInteger> gasPrice = new MutableLiveData<>();
    private MutableLiveData<BigInteger> gasLimit = new MutableLiveData<>();

    public GasSettingsViewModel() {
        gasPrice.setValue(new BigInteger("0"));
        gasLimit.setValue(new BigInteger("0"));
    }

    public MutableLiveData<BigInteger> gasPrice() {
        return gasPrice;
    }

    public MutableLiveData<BigInteger> gasLimit() {
        return gasLimit;
    }

    public BigInteger networkFee() {
        return gasPrice.getValue().multiply(gasLimit.getValue());
    }
}
