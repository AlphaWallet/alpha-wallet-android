package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class WalletConnectV2ViewModel extends BaseViewModel
{
    private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    private final FetchWalletsInteract fetchWalletsInteract;
    private final GenericWalletInteract genericWalletInteract;

    public LiveData<Wallet[]> wallets()
    {
        return wallets;
    }

    @Inject
    WalletConnectV2ViewModel(FetchWalletsInteract fetchWalletsInteract, GenericWalletInteract genericWalletInteract)
    {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.genericWalletInteract = genericWalletInteract;

        fetchWallets();
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

    public void fetchWallets()
    {
        disposable = fetchWalletsInteract
                .fetch()
                .subscribe(this::onWallets, this::onError);
    }

    private void onWallets(Wallet[] wallets)
    {
        this.wallets.postValue(wallets);
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }
}
