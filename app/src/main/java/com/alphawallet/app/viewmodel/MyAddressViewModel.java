package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.TextUtils;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.util.AWEnsResolver;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MyAddressViewModel extends BaseViewModel {
    private final String TAG = MyAddressViewModel.class.getSimpleName();
    private static final long CHECK_ETHPRICE_INTERVAL = 60;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<TokenTicker> updateToken = new MutableLiveData<>();

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
    public LiveData<TokenTicker> updateToken() {
        return updateToken;
    }

    public NetworkInfo[] getNetworkList() {
        return ethereumNetworkRepository.getAvailableNetworkList();
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

    public void startEthereumTicker(final Token token)
    {
        if (token.isEthereum())
        {
            disposable = Observable.interval(0, CHECK_ETHPRICE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> Single.fromCallable(() -> ethereumNetworkRepository.getTokenTicker(token))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(updateToken::postValue, this::onError)).subscribe();
        }
    }

    public Single<String> resolveEns(String address)
    {
        GasService gasService = new GasService(this.ethereumNetworkRepository);
        return Single.fromCallable(() -> {
            AWEnsResolver resolver = new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), gasService);
            String walletENSName = "";
            try
            {
                walletENSName = resolver.reverseResolve(address);
                if (!TextUtils.isEmpty(walletENSName))
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolver.resolve(walletENSName);
                    if (!resolveAddress.equalsIgnoreCase(address))
                    {
                        walletENSName = null;
                    }
                }
            }
            catch (Exception e)
            {
                walletENSName = null;
            }
            return walletENSName;
        });
    }
}
