package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QrUrlResult;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.SendActivity;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AddTokenViewModel extends BaseViewModel {

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();
    private final MutableLiveData<Integer> switchNetwork = new MutableLiveData<>();
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final SetupTokensInteract setupTokensInteract;
    private final AddTokenInteract addTokenInteract;
    private final GenericWalletInteract genericWalletInteract;
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
            GenericWalletInteract genericWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokenInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.addTokenInteract = addTokenInteract;
        this.genericWalletInteract = genericWalletInteract;
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
    public MutableLiveData<Token> tokenFinalised() { return finalisedToken; }

    public void save(String address, String symbol, int decimals, String name, int chainId) {
        TokenInfo tokenInfo = getTokenInfo(address, symbol, decimals, name, chainId);
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, wallet.getValue()))
                .flatMap(token -> fetchTokensInteract.updateDefaultBalance(token, wallet.getValue()))
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
        assetDefinitionService.getAssetDefinition(token.tokenInfo.chainId, token.getAddress());
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

    public int getSelectedChain()
    {
        return primaryChainId;
    }

    private void setupToken(int chainId, String addr) {
        disposable = setupTokensInteract
                .update(addr, chainId)
                .subscribe(this::onTokensSetup, this::onError, this::onFetchTokensCompletable);
    }

    public void fetchToken(int chainId, String addr)
    {
        setupTokensInteract.update(addr, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotTokenUpdate, this::onError).isDisposed();
    }

    private void gotTokenUpdate(TokenInfo tokenInfo)
    {
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, wallet.getValue()))
                .flatMap(token -> fetchTokensInteract.updateDefaultBalance(token, wallet.getValue()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::resumeSend, this::onError);
    }

    private void resumeSend(Token token)
    {
        tokensService.addToken(token);
        finalisedToken.postValue(token);
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
        disposable = genericWalletInteract.find()
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

    public void showSend(Context ctx, QrUrlResult result, Token token)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        boolean sendingTokens = (result.getFunction() != null && result.getFunction().length() > 0);
        String address = wallet.getValue().address;
        int decimals = 18;

        if (sendingTokens)
        {
            address = result.getAddress();
            decimals = token.tokenInfo.decimals;
        }

        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getNetworkByChain(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, wallet.getValue());
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
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

    public void testNetworks(String address, NetworkInfo networkInfo)
    {
        testAddress = address;
        foundNetwork = false;
        networkCount = ethereumNetworkRepository.getAvailableNetworkList().length;
        //first test the network selected, then do all the others
        scanNetworksDisposable = fetchTokensInteract.getContractResponse(testAddress, networkInfo.chainId, "name")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkSelectedNetwork, this::onTestError);
    }

    private void checkSelectedNetwork(ContractResult result)
    {
        if (!result.name.equals(TokenRepository.INVALID_CONTRACT))
        {
            foundNetwork = true;
            switchNetwork.postValue(result.chainId);
            setupToken(result.chainId, testAddress);
        }
        else
        {
            //test all the other networks
            List<Integer> networkIds = getNetworkIds();
            networkIds.remove((Integer)result.chainId);

            scanNetworksDisposable = Observable.fromCallable(() -> networkIds)
                    .flatMapIterable(networkId -> networkId)
                    .filter(networkId -> !foundNetwork)
                    .flatMap(networkId -> fetchTokensInteract.getContractResponse(testAddress, networkId, "name"))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::testNetworkResult, this::onTestError);
        }
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

    public void showImportLink(Context context, String importTxt)
    {
        Intent intent = new Intent(context, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, importTxt);
        context.startActivity(intent);
    }

    public Token getToken(int chainId, String address)
    {
        return tokensService.getToken(chainId, address);
    }
}
