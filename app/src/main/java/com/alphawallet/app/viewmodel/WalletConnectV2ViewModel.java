package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class WalletConnectV2ViewModel extends BaseViewModel
{
    private final GenericWalletInteract genericWalletInteract;
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    @Inject
    WalletConnectV2ViewModel(GenericWalletInteract genericWalletInteract)
    {
        this.genericWalletInteract = genericWalletInteract;
        fetchDefaultWallet();
    }

    private void fetchDefaultWallet()
    {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        this.defaultWallet.postValue(wallet);
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }
}
