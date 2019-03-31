package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import android.content.Intent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.AddTokenActivity;
import io.stormbird.wallet.ui.SendActivity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AddTokenViewModel extends BaseViewModel {

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();
    private final MutableLiveData<Integer> switchNetwork = new MutableLiveData<>();

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final SetupTokensInteract setupTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private boolean foundNetwork;
    private int networkCount;
    private int primaryChainId = 1;
    private String testAddress;

    private final MutableLiveData<Boolean> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> update = new MutableLiveData<>();

    @Nullable
    Disposable scanNetworksDisposable;

    AddTokenViewModel(
            AddTokenInteract addTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokenInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.addTokenInteract = addTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokenInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

    public void save(String address, String symbol, int decimals, String name, int chainId) {
        TokenInfo tokenInfo = getTokenInfo(address, symbol, decimals, name, chainId);
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, wallet.getValue()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onSaved, error -> onInterfaceCheckError(error, tokenInfo));
    }

    //fallback in case interface spec check throws an error.
    //If any token data was picked up then default to ERC20 token.
    private void onInterfaceCheckError(Throwable throwable, TokenInfo tokenInfo)
    {
        if ((tokenInfo.name != null && tokenInfo.name.length() > 0)
            || (tokenInfo.symbol != null && tokenInfo.symbol.length() > 0))
        disposable = addTokenInteract.add(tokenInfo, ContractType.ERC20, wallet.getValue())
                .subscribe(this::onSaved, this::onError);
    }

    private void onSaved(Token token)
    {
        assetDefinitionService.getAssetDefinition(token.getAddress());
        tokensService.addToken(token);
        progress.postValue(false);
        result.postValue(true);
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (scanNetworksDisposable != null && !scanNetworksDisposable.isDisposed()) scanNetworksDisposable.dispose();
    }

    private TokenInfo getTokenInfo(String address, String symbol, int decimals, String name, int chainId)
    {
        return new TokenInfo(address, name, symbol, decimals, true, chainId);
    }

    public void setPrimaryChain(int chainId)
    {
        primaryChainId = chainId;
    }

    private void setupToken(int chainId, String addr) {
        disposable = setupTokensInteract
                .update(addr, chainId)
                .subscribe(this::onTokensSetup, this::onError, this::onFetchTokensCompletable);
    }

    public LiveData<Boolean> result() {
        return result;
    }

    public LiveData<Boolean> update() {
        return update;
    }

    public LiveData<Integer> switchNetwork() { return switchNetwork; }

    public NetworkInfo getNetworkInfo(int chainId) { return ethereumNetworkRepository.getNetworkByChain(chainId); }

    private void findWallet()
    {
        disposable = findDefaultWalletInteract.find()
                .subscribe(wallet::setValue, this::onError);
    }

    private void onTokensSetup(TokenInfo tokenData) {
        tokenInfo.setValue(tokenData);
    }

    private void onFetchTokensCompletable() {
        update.postValue(true);
    }

    public LiveData<TokenInfo> tokenInfo() {
        return tokenInfo;
    }

    public void prepare()
    {
        findWallet();
        testAddress = null;
    }

    public void showSend(Context ctx, QrUrlResult result)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, false);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, wallet.getValue().address);
        intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getNetworkByChain(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, 18);
        intent.putExtra(C.Key.WALLET, wallet.getValue());
        intent.putExtra(C.EXTRA_TOKEN_ID, tokensService.getToken(result.chainId, wallet.getValue().address));
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public NetworkInfo[] getNetworkList() {
        return ethereumNetworkRepository.getAvailableNetworkList();
    }

    public NetworkInfo getNetwork(int chainId)
    {
        NetworkInfo networkInfo = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (networkInfo != null)
        {
            ethereumNetworkRepository.setDefaultNetworkInfo(networkInfo);
            return networkInfo;
        }
        else
        {
            return null;
        }
    }

    private List<Integer> getNetworkIds()
    {
        List<Integer> networkIds = new ArrayList<>();
        networkIds.add(primaryChainId); //test selected chain first
        for (NetworkInfo networkInfo : ethereumNetworkRepository.getAvailableNetworkList())
        {
            if (!networkIds.contains(networkInfo.chainId)) networkIds.add(networkInfo.chainId);
        }
        return networkIds;
    }

    public void testNetworks(String address)
    {
        testAddress = address;
        foundNetwork = false;
        networkCount = ethereumNetworkRepository.getAvailableNetworkList().length;
        //test all the networks
        scanNetworksDisposable = Observable.fromCallable(this::getNetworkIds)
                .flatMapIterable(networkId -> networkId)
                .filter(networkId -> !foundNetwork)
                .flatMap(networkId -> fetchTokensInteract.getContractResponse(testAddress, networkId, "name"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::testNetworkResult, this::onTestError);
    }

    private void onTestError(Throwable throwable)
    {
        checkNetworkCount();
        onError(throwable);
    }

    private void testNetworkResult(ContractResult result)
    {
        if (!foundNetwork && !result.name.equals(TokenRepository.INVALID_CONTRACT))
        {
            foundNetwork = true;
            if (scanNetworksDisposable != null && !scanNetworksDisposable.isDisposed()) scanNetworksDisposable.dispose(); //stop scanning
            switchNetwork.postValue(result.chainId);
            setupToken(result.chainId, testAddress);
        }
        else
        {
            checkNetworkCount();
        }
    }

    private void checkNetworkCount()
    {
        networkCount--;
        if (networkCount == 0 && !foundNetwork)
        {
            testAddress = null;
            update.postValue(false);
        }
    }
}
