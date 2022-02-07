package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;

import javax.inject.Inject;

import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class WalletConnectV2ViewModel extends BaseViewModel
{
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;

    @Inject
    WalletConnectV2ViewModel(GenericWalletInteract genericWalletInteract)
    {
        this.genericWalletInteract = genericWalletInteract;
    }

    public void prepare()
    {
        genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.postValue(wallet);
    }

    public MutableLiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }
}
