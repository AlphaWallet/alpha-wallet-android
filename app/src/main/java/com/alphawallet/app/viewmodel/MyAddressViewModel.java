package com.alphawallet.app.viewmodel;

<<<<<<< HEAD
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
=======
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
>>>>>>> e3074436a... Attempt to upgrade to AndroidX

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;

public class MyAddressViewModel extends BaseViewModel {
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    MyAddressViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenRepositoryType tokenRepository) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
    }

    public TokenRepositoryType getTokenRepository() {
        return tokenRepository;
    }

    public EthereumNetworkRepositoryType getEthereumNetworkRepository() {
        return ethereumNetworkRepository;
    }

    public void prepare() {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public NetworkInfo setNetwork(int chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info != null)
        {
            ethereumNetworkRepository.setDefaultNetworkInfo(info);
            defaultNetwork.postValue(info);
            return info;
        }

        return null;
    }
}
