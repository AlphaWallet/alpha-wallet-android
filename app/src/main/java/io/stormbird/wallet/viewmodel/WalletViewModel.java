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
import io.stormbird.wallet.entity.ERC721Token;
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

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> checkTokens = new MutableLiveData<>();
    private final MutableLiveData<List<String>> removeTokens = new MutableLiveData<>();

    private final MutableLiveData<String> checkAddr = new MutableLiveData<>();

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
    public LiveData<String> checkAddr() { return checkAddr; }
    public LiveData<List<String>> removeTokens() { return removeTokens; }

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
    public void clearProcess()
    {
        tokensService.clearTokens();
        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
        }
        if (fetchTokenBalanceDisposable != null && !fetchTokenBalanceDisposable.isDisposed())
        {
            fetchTokenBalanceDisposable.dispose();
        }
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

        if (defaultNetwork.getValue() != null && defaultWallet.getValue() != null)
        {
            tokenCache = null;
            updateTokens = fetchTokensInteract.fetchStoredWithEth(defaultNetwork.getValue(), defaultWallet.getValue())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokens, this::onError, this::fetchFromOpensea);
        }
        else
        {
            //called fetch tokens but don't have any wallet yet - view is not prepared
            prepare();
        }
    }

    private void onTokens(Token[] tokens)
    {
        tokensService.addTokens(tokens);
    }

    private boolean firstRunDebugTest = true;

    private void fetchFromOpensea()
    {
        List<Token> serviceList = tokensService.getAllLiveTokens();
        tokenCache = serviceList.toArray(new Token[0]);
        tokens.postValue(tokenCache);
        //get the XML address
        setContractAddresses();

        if (updateTokens != null) updateTokens.dispose();

        if (defaultNetwork.getValue() != null && defaultNetwork.getValue().isMainNetwork)
        {
            updateTokens = openseaService.getTokens(defaultWallet.getValue().address)
                    //openseaService.getTokens("0x51A9f155405Ea594d881fE9c1f1eb38F003B0A57") //"0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63"
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::gotOpenseaTokens, this::onError);
        }
        else
        {
            onFetchTokensCompletable();
        }
    }

    private void gotOpenseaTokens(Token[] tokens)
    {
        if (updateTokens != null) updateTokens.dispose();

        //update the display list for token removals
        List<String> removedTokens = tokensService.getRemovedTokensOfClass(tokens, ERC721Token.class);

        if (!removedTokens.isEmpty()) removeTokens.postValue(removedTokens); //remove from UI

        tokensService.clearBalanceOf(ERC721Token.class);

        for (Token t : tokens)
        {
            tokensService.addTokenUnchecked(t);
        }

        List<Token> tokenList = tokensService.getAllClass(ERC721Token.class);

        //store these tokens
        updateTokens = addTokenInteract.addERC721(defaultWallet.getValue(), tokenList.toArray(new Token[0]))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::storedTokens, this::onError);

        tokenList = tokensService.getAllLiveTokens();
        tokenCache = tokenList.toArray(new Token[0]);
    }

    private void storedTokens(Token[] tokens)
    {
        Log.d("WVM", "Stored " + tokens.length);
        //if (updateTokens != null && !updateTokens.isDisposed()) updateTokens.dispose();
        onFetchTokensCompletable();
    }

    private void onFetchTokensCompletable()
    {
        progress.postValue(false);
        tokens.postValue(tokenCache);

        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
            updateTokens = null;
        }

        updateTokenBalances();

        // Check contracts that returned a null but we didn't see them destroyed yet.
        // Sometimes the network times out or some other issue.
        checkTokensDisposable = Observable.fromCallable(tokensService::getAllTokens)
                .flatMapIterable(token -> token)
                .filter(token -> (token.tokenInfo.name == null && !token.isTerminated()))
                .flatMap(token -> fetchTokensInteract.getTokenInfo(token.getAddress()))
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
        if (fetchTokenBalanceDisposable != null && !fetchTokenBalanceDisposable.isDisposed())
        {
            fetchTokenBalanceDisposable.dispose();
        }

        NetworkInfo info = defaultNetwork.getValue();
        Wallet wallet = defaultWallet.getValue();
        if (info != null && wallet != null)
        {
            fetchTokenBalanceDisposable = Observable.interval(0, GET_BALANCE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> Observable.fromCallable(tokensService::getAllTokens)
                            .flatMapIterable(token -> token)
                            .filter(token -> (token.tokenInfo.name != null && !token.isTerminated() && !token.independentUpdate()))
                            .flatMap(token -> fetchTokensInteract.updateDefaultBalance(token, info, wallet))
                            .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onTokenBalanceUpdate, this::onError, this::onFetchTokensBalanceCompletable)).subscribe();
        }
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
        tokensService.setCurrentNetwork(networkInfo.chainId);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        tokensService.setCurrentAddress(wallet.address);
        defaultWallet.setValue(wallet);
    }

    public Single<NetworkInfo> getNetwork()
    {
        if (defaultNetwork().getValue() != null)
        {
            return Single.fromCallable(() -> defaultNetwork().getValue());
        }
        else
        {
            return findDefaultNetworkInteract.find();
        }
    }

    private Single<AccountData> getWallet(NetworkInfo networkInfo)
    {
        if (defaultWallet().getValue() != null)
        {
            return Single.fromCallable(() -> {
                return new AccountData(networkInfo, defaultWallet.getValue());
            });
        }
        else
        {
            return findDefaultWalletInteract.find()
                    .map(wallet -> new AccountData(networkInfo, wallet) );
        }
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

    private void setContractAddresses()
    {
        disposable = fetchAllContractAddresses()
                .flatMap(tokensService::reduceToUnknown)
                .flatMapIterable(address -> address)
                .flatMap(setupTokensInteract::addToken)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, defaultWallet.getValue()))
                .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
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

    public Token getTokenFromService(Token token)
    {
        Token serviceToken = tokensService.getToken(token.getAddress());
        return (serviceToken != null) ? serviceToken : token;
    }

    private class AccountData
    {
        AccountData(NetworkInfo network, Wallet wallet) { this.network = network; this.wallet = wallet; }
        public NetworkInfo network;
        public Wallet wallet;
    }
}
