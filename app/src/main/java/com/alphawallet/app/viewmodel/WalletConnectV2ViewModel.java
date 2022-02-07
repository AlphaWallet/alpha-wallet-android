package com.alphawallet.app.viewmodel;

import android.util.Log;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

public class WalletConnectV2ViewModel extends BaseViewModel
{
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final GenericWalletInteract genericWalletInteract;

    public WalletConnectV2ViewModel(GenericWalletInteract genericWalletInteract)
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

    public void pair(String url)
    {
        Log.d("seaborn", "Pair: " + url);
        WalletConnect.Params.Pair pair = new WalletConnect.Params.Pair(url);
        WalletConnectClient.INSTANCE.pair(pair, getPairing());
    }

    @NonNull
    private WalletConnect.Listeners.Pairing getPairing()
    {
        return new WalletConnect.Listeners.Pairing()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.SettledPairing settledPairing)
            {
            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {
            }
        };
    }

}
