package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.token.entity.ContractAddress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AddTokenViewModel extends BaseViewModel {

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<TokenInfo> tokenInfo = new MutableLiveData<>();
    private final MutableLiveData<Integer> switchNetwork = new MutableLiveData<>();
    private final MutableLiveData<Token> finalisedToken = new MutableLiveData<>();
    private final MutableLiveData<Token> tokentype = new MutableLiveData<>();
    private final MutableLiveData<Token> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noContract = new MutableLiveData<>();

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
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

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }
    public MutableLiveData<Token> tokenFinalised() { return finalisedToken; }
    public MutableLiveData<Token> tokenType() { return tokentype; }
    public MutableLiveData<Boolean> noContract() { return noContract; }
    public LiveData<Token> result() { return result; }
    public LiveData<Integer> switchNetwork() { return switchNetwork; }
    public LiveData<TokenInfo> tokenInfo() {
        return tokenInfo;
    }

    @Nullable
    Disposable scanNetworksDisposable;

    AddTokenViewModel(
            AddTokenInteract addTokenInteract,
            GenericWalletInteract genericWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.addTokenInteract = addTokenInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public void save(int chainId, String address, String name, String symbol, int decimals, ContractType contractType)
    {
        //update token details as entered
        TokenInfo tf = new TokenInfo(address, name, symbol, decimals, true, chainId);
        addTokenInteract.add(tf, contractType, wallet.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result::postValue, this::onError)
                .isDisposed();
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (scanNetworksDisposable != null && !scanNetworksDisposable.isDisposed()) scanNetworksDisposable.dispose();
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
        disposable = tokensService
                .update(addr, chainId)
                .subscribe(this::onTokensSetup, this::onError);
    }

    public void fetchToken(int chainId, String addr)
    {
        tokensService.update(addr, chainId)
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
        finalisedToken.postValue(token);
    }

    public NetworkInfo getNetworkInfo(int chainId) { return ethereumNetworkRepository.getNetworkByChain(chainId); }

    private void findWallet()
    {
        disposable = genericWalletInteract.find()
                .subscribe(wallet::setValue, this::onError);
    }

    private void onTokensSetup(TokenInfo tokenData) {
        tokenInfo.postValue(tokenData);
        disposable = fetchTransactionsInteract.queryInterfaceSpec(tokenData).toObservable()
                .flatMap(contractType -> addTokenInteract.add(tokenData, contractType, wallet.getValue()))
                //.flatMap(token -> fetchTokensInteract.updateDefaultBalance(token, wallet.getValue()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokentype::postValue, error -> tokenTypeError(error, tokenData));
    }

    private void tokenTypeError(Throwable throwable, TokenInfo data)
    {
        Token badToken = new Token(data, BigDecimal.ZERO, 0, "", ContractType.NOT_SET);
        tokentype.postValue(badToken);
    }

    public void prepare()
    {
        findWallet();
        testAddress = null;
    }

    public void showSend(Context ctx, QRResult result, Token token)
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

    private List<Integer> getNetworkIds()
    {
        List<Integer> networkIds = new ArrayList<>();
        networkIds.add(primaryChainId); //test selected chain first
        for (int chainId : tokensService.getNetworkFilters())
        {
            if (!networkIds.contains(chainId)) networkIds.add(chainId);
        }

        //Now scan unselected networks
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

    private void checkSelectedNetwork(ContractLocator result)
    {
        if (!result.address.equals(TokenRepository.INVALID_CONTRACT))
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
            networkCount--;

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

    private void testNetworkResult(ContractLocator result)
    {
        if (!foundNetwork && !result.address.equals(TokenRepository.INVALID_CONTRACT))
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
            noContract.postValue(true);
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
