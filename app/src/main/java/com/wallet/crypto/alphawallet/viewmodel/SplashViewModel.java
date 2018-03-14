package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchWalletsInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;

import static com.wallet.crypto.alphawallet.C.DEFAULT_NETWORK;
import static com.wallet.crypto.alphawallet.C.OVERRIDE_DEFAULT_NETWORK;

public class SplashViewModel extends ViewModel {
    private final FetchWalletsInteract fetchWalletsInteract;
    private final EthereumNetworkRepositoryType networkRepository;

    private MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();

    SplashViewModel(FetchWalletsInteract fetchWalletsInteract,
                    EthereumNetworkRepositoryType networkRepository) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.networkRepository = networkRepository;

        fetchWalletsInteract
                .fetch()
                .subscribe(wallets::postValue, this::onError);
    }

    public void setOverrideNetwork() {
        if (OVERRIDE_DEFAULT_NETWORK) {
            NetworkInfo[] networks = networkRepository.getAvailableNetworkList();
            for (NetworkInfo networkInfo : networks) {
                if (networkInfo.name.equals(DEFAULT_NETWORK)) {
                    networkRepository.setDefaultNetworkInfo(networkInfo);
                    break;
                }
            }
        }
    }

    private void onError(Throwable throwable) {
        wallets.postValue(new Wallet[0]);
    }

    public LiveData<Wallet[]> wallets() {
        return wallets;
    }
}
