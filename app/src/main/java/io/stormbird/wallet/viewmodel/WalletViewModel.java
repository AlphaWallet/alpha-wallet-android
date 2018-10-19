package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.interact.SetupTokensInteract;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ChangeTokenCollectionRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.service.TokensService;

import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;

public class WalletViewModel extends BaseViewModel
{
    private static final long GET_BALANCE_INTERVAL = 10;

    //    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> checkTokens = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final AssetDefinitionService assetDefinitionService;
    private final OpenseaService openseaService;
    private final TokensService tokensService;

    private Token[] tokenCache = null;
    private boolean isVisible = false;

    @Nullable
    private Disposable fetchTokenBalanceDisposable;
    @Nullable
    private Disposable updateTokens;
    @Nullable
    private Disposable checkTokensDisposable;

    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter,
            AssetDisplayRouter assetDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            AddTokenInteract addTokenInteract,
            SetupTokensInteract setupTokensInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            OpenseaService openseaService)
    {
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.openseaService = openseaService;
        this.tokensService = tokensService;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }
    public LiveData<BigDecimal> total() {
        return total;
    }
    public LiveData<Token> tokenUpdate() { return tokenUpdate; }
    public LiveData<Boolean> endUpdate() { return checkTokens; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (fetchTokenBalanceDisposable != null && !fetchTokenBalanceDisposable.isDisposed())
        {
            fetchTokenBalanceDisposable.dispose();
        }
        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
        }
    }

    //we changed wallets or network, ensure we clean up before displaying new data
    public void abortAndRestart()
    {
        defaultWallet.setValue(null);
        tokensService.clearTokens();
        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
        }
        if (fetchTokenBalanceDisposable != null && !fetchTokenBalanceDisposable.isDisposed())
        {
            fetchTokenBalanceDisposable.dispose();
        }

        fetchTokens();
    }

    public void reloadTokens()
    {
        assetDefinitionService.clearCheckTimes();
        fetchTokens();
    }

    public void fetchTokens()
    {
        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
        }

        updateTokens = getWallet().toObservable()
                .flatMap(fetchTokensInteract::fetchStoredWithEth)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTokens, this::onError, this::fetchFromOpensea);
    }

    private void onTokens(Token[] tokens)
    {
        tokensService.addTokens(tokens);
    }

    private void fetchFromOpensea()
    {
        List<Token> serviceList = tokensService.getAllTokens();
        tokenCache = serviceList.toArray(new Token[serviceList.size()]);

        progress.postValue(false);
        tokens.postValue(tokenCache);

        if (updateTokens != null) updateTokens.dispose();
        //updateTokens = openseaService.getTokens(defaultWallet.getValue().address)
        //ERC721 Testing
        //0x07b99b5a4093be2c4465d55fcaad50a3cb61447a
        //Wallet tester = new Wallet("0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63"); //account containing kitties 0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63
        updateTokens = getWallet()
                .flatMap(wallet -> openseaService.getTokens(wallet.address))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotOpenseaTokens, this::onError);
    }

    private void gotOpenseaTokens(Token[] tokens)
    {
        if (updateTokens != null) updateTokens.dispose();

        for (Token t : tokens)
        {
            tokensService.addToken(t);
        }

        List<Token> serviceList = tokensService.getAllTokens();
        tokenCache = serviceList.toArray(new Token[serviceList.size()]);
        onFetchTokensCompletable();
    }

    private void onFetchTokensCompletable()
    {
        progress.postValue(false);
        tokens.postValue(tokenCache);
        //test to see if we should continue to update
        if (!isVisible && updateTokens != null)
        {
            updateTokens.dispose();
            updateTokens = null;
        }
        else if (fetchTokenBalanceDisposable == null || fetchTokenBalanceDisposable.isDisposed())
        {
            updateTokenBalances();
        }

        // Check contracts that returned a null but we didn't see them destroyed yet.
        // Sometimes the network times out or some other issue.
        checkTokensDisposable = Observable.fromCallable(tokensService::getAllTokens)
                .flatMapIterable(token -> token)
                .filter(token -> (token.tokenInfo.name == null && !token.isTerminated()))
                .flatMap(token-> fetchTokensInteract.getTokenInfo(token.getAddress()))
                .filter(tokenInfo -> (tokenInfo.name != null))
                .subscribeOn(Schedulers.io())
                .subscribe(addTokenInteract::addS, this::tkError,
                           () -> { if (checkTokensDisposable != null) checkTokensDisposable.dispose(); });
    }

    private void tkError(Throwable throwable)
    {
        if (checkTokensDisposable != null) checkTokensDisposable.dispose();
    }

    private void updateTokenBalances()
    {
        fetchTokenBalanceDisposable = Observable.interval(0, GET_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> Observable.fromCallable(tokensService::getAllTokens)
                        .flatMapIterable(token -> token)
                        .filter(token -> (token.tokenInfo.name != null && !token.isTerminated() && !token.independentUpdate()))
                        .map(fetchTokensInteract::updateDefaultBalance)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTokenBalanceUpdate, this::onError, this::onFetchTokensBalanceCompletable)).subscribe();
    }

    private void onTokenBalanceUpdate(Token token)
    {
        tokenUpdate.postValue(token);
        tokensService.addToken(token);
    }

    private void onFetchTokensBalanceCompletable()
    {
        progress.postValue(false);
        if (tokenCache != null && tokenCache.length > 0)
        {
            checkTokens.postValue(true);
        }
        else
        {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }

        if (!isVisible && fetchTokenBalanceDisposable != null)
        {
            fetchTokenBalanceDisposable.dispose();
            fetchTokenBalanceDisposable = null;
        }
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    //NB: This function is used to calculate total value of all tokens plus eth.
    //TODO: On mainnet, get tickers for all token values and calculate the overall $ value of all tokens + eth
//    private void showTotalBalance(Token[] tokens) {
//        BigDecimal total = new BigDecimal("0");
//        for (Token token : tokens) {
//            if (token.balance != null && token.ticker != null
//                    && token.balance.compareTo(BigDecimal.ZERO) != 0) {
//                BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, token.tokenInfo.decimals));
//                BigDecimal ethBalance = token.tokenInfo.decimals > 0
//                        ? token.balance.divide(decimalDivisor)
//                        : token.balance;
//                total = total.add(ethBalance.multiply(new BigDecimal(token.ticker.price)));
//            }
//        }
//        total = total.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
//        if (total.compareTo(BigDecimal.ZERO) == 0) {
//            total = null;
//        }
//        this.total.postValue(total);
//    }

    public void showAddToken(Context context) {
        addTokenRouter.open(context);
    }

    @Override
    public void showSendToken(Context context, String address, String symbol, int decimals, Token token) {
        boolean isToken = true;
        if (address.equalsIgnoreCase(defaultWallet().getValue().address)) isToken = false;
        sendTokenRouter.open(context, address, symbol, decimals, isToken, defaultWallet.getValue(), token);
    }

    @Override
    public void showRedeemToken(Context context, Token token) {
        assetDisplayRouter.open(context, token);
    }

    public void showEditTokens(Context context) {
        changeTokenCollectionRouter.open(context, defaultWallet.getValue());
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Map<String, String>> defaultWalletBalance() {
        return defaultWalletBalance;
    }

    public void prepare() {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public Single<Wallet> getWallet()
    {
        if (defaultWallet().getValue() != null)
        {
            return Single.fromCallable(() -> defaultWallet().getValue());
        }
        else
            return findDefaultNetworkInteract.find()
                    .flatMap(networkInfo -> findDefaultWalletInteract
                            .find());
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }

    public void reStartTokenUpdate()
    {
        if (updateTokens == null || updateTokens.isDisposed())
        {
            fetchTokens();
        }
    }

    public void setContractAddresses()
    {
        disposable = fetchAllContractAddresses()
                .flatMapIterable(address -> address)
                .flatMap(setupTokensInteract::addToken)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, defaultWallet.getValue()))
                .subscribeOn(Schedulers.io())
                .subscribe(this::finishedImport, this::onTokenAddError);
    }

    private Observable<List<String>> fetchAllContractAddresses()
    {
        return Observable.fromCallable(() -> {
            //populate contracts from service
            List<String> contracts = assetDefinitionService.getAllContracts(defaultNetwork.getValue().chainId);

            return contracts;
        });
    }

    private void onTokenAddError(Throwable throwable)
    {
        //cannot add the token until we get internet connection
        Log.d("WVM", "Wait for internet");
    }

    private void finishedImport(Token token)
    {
        Log.d("WVM", "Added " + token.tokenInfo.name);
    }
}
