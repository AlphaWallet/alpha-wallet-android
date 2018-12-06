package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ChangeTokenCollectionRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.service.TokensService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;

public class WalletViewModel extends BaseViewModel implements Runnable
{
    private static final long GET_BALANCE_INTERVAL = 15;

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
    private final FetchTransactionsInteract fetchTransactionsInteract;

    private Token[] tokenCache = null;
    private boolean isVisible = false;
    private boolean firstRun = true;
    private int checkCounter;

    @Nullable
    private Disposable balanceTimerDisposable;
    @Nullable
    private Disposable updateTokens;
    @Nullable
    private Disposable nullTokensCheckDisposable;
    @Nullable
    private Disposable balanceCheckDisposable;

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
            OpenseaService openseaService,
            FetchTransactionsInteract fetchTransactionsInteract)
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
        this.fetchTransactionsInteract = fetchTransactionsInteract;
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
    }

    //we changed wallets or network, ensure we clean up before displaying new data
    public void clearProcess()
    {
        tokensService.clearTokens();
        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
        }
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable.dispose();
        }
        if (balanceCheckDisposable != null && !balanceCheckDisposable.isDisposed())
        {
            balanceCheckDisposable.dispose();
            balanceCheckDisposable = null;
        }
    }

    public void reloadTokens()
    {
        assetDefinitionService.clearCheckTimes();
        clearProcess();
        fetchTokens();
    }

    public void fetchTokens()
    {
        if (defaultNetwork.getValue() != null && defaultWallet.getValue() != null)
        {
            tokenCache = null;
            checkCounter = 0;
            tokensService.setCurrentAddress(defaultWallet.getValue().address);
            tokensService.setCurrentNetwork(defaultNetwork.getValue().chainId);
            updateTokens = fetchTokensInteract.fetchStoredWithEth(defaultNetwork.getValue(), defaultWallet.getValue())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokens, this::onTokenFetchError, this::fetchFromOpensea);
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

    private void onTokenFetchError(Throwable throwable)
    {
        //We encountered an unknown issue during token fetch
        //This is most likely due to a balance recording error
        //log the exception for reference
        Crashlytics.logException(throwable);
        throwable.printStackTrace();
        onError(throwable);
    }


    /**
     * Stage 2: Fetch opensea tokens (if on mainnet)
     */
    private void fetchFromOpensea()
    {
        List<Token> serviceList = tokensService.getAllLiveTokens();
        tokenCache = serviceList.toArray(new Token[0]);

        if (updateTokens != null) updateTokens.dispose();

        if (defaultNetwork.getValue() != null && defaultNetwork.getValue().isMainNetwork)
        {
            tokens.postValue(tokenCache);

            updateTokens = openseaService.getTokens(defaultWallet.getValue().address)
                    //openseaService.getTokens("0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::gotOpenseaTokens, this::onOpenseaError);
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
        //Update the tokenCache with ERC721 tokens ready for the display refresh
        tokenCache = tokensService.getAllLiveTokens().toArray(new Token[0]);

        //store these tokens
        updateTokens = addTokenInteract.addERC721(defaultWallet.getValue(), tokensService.getAllClass(ERC721Token.class).toArray(new Token[0]))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::storedTokens, this::onError);
    }

    private void onOpenseaError(Throwable throwable)
    {
        Crashlytics.logException(throwable);
        throwable.printStackTrace();
        onError(throwable);
    }

    private void storedTokens(Token[] tokens)
    {
        Log.d("WVM", "Stored " + tokens.length);
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
    }

    /**
     * Start the token checking thread
     */
    private void updateTokenBalances()
    {
        balanceTimerDisposable = Observable.interval(0, GET_BALANCE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> updateBalances()).subscribe();
    }

    private void updateBalances()
    {
        if (balanceCheckDisposable == null || balanceCheckDisposable.isDisposed())
        {
            run();
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

    public void prepare()
    {
        if (defaultNetwork.getValue() == null || defaultWallet.getValue() == null)
        {
            progress.postValue(true);
            disposable = findDefaultNetworkInteract
                    .find()
                    .subscribe(this::onDefaultNetwork, this::onError);
        }
        else if (tokensService.getAllTokens().size() == 0)
        {
            fetchTokens();
        }
        else if (balanceTimerDisposable == null || balanceTimerDisposable.isDisposed())
        {
            updateTokenBalances();
        }
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

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }

    private void setContractAddresses()
    {
        disposable = fetchAllContractAddresses()
                .flatMap(tokensService::reduceToUnknown)
                .flatMapIterable(address -> address)
                .flatMap(tokenAddress -> setupTokensInteract.addToken(tokenAddress))
                .flatMap(fetchTransactionsInteract::queryInterfaceSpecForService)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.address)))
                .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
                .filter(token -> (token != null && (token.tokenInfo.name != null || token.tokenInfo.symbol != null)))
                .subscribeOn(Schedulers.io())
                .subscribe(this::finishedImport, this::onTokenAddError, this::finishedXMLSetup);
    }

    private void finishedImport(Token token)
    {
        tokensService.addToken(token);
    }

    private void onTokenAddError(Throwable throwable)
    {
        //cannot add the token until we get internet connection
        Log.d("WVM", "Wait for internet");
    }

    private void finishedXMLSetup()
    {
        // Check contracts that returned a null but we didn't see them destroyed yet.
        // Sometimes the network times out or some other issue.
        nullTokensCheckDisposable = Observable.fromCallable(tokensService::getAllTokens)
                .flatMapIterable(token -> token)
                .filter(token -> (token.tokenInfo.name == null && !token.isTerminated()))
                .concatMap(token -> fetchTokensInteract.getTokenInfo(token.getAddress()))
                .filter(tokenInfo -> (tokenInfo.name != null))
                .subscribeOn(Schedulers.io())
                .subscribe(addTokenInteract::addS, this::onError,
                           () -> { if (nullTokensCheckDisposable != null) nullTokensCheckDisposable.dispose(); });
    }

    private Observable<List<String>> fetchAllContractAddresses()
    {
        return Observable.fromCallable(() -> {
            //populate contracts from service
            List<String> contracts = assetDefinitionService.getAllContracts(defaultNetwork.getValue().chainId);

            return contracts;
        });
    }

    public Token getTokenFromService(Token token)
    {
        Token serviceToken = tokensService.getToken(token.getAddress());
        return (serviceToken != null) ? serviceToken : token;
    }

    @Override
    public void run()
    {
        if (defaultNetwork.getValue() != null && defaultWallet.getValue() != null)
        {
            balanceCheckDisposable = Observable.fromCallable(tokensService::getAllLiveTokens)
                    .flatMapIterable(token -> token)
                    .filter(token -> (token.tokenInfo.name != null && !token.isTerminated() && !token.independentUpdate())) //don't check terminated or ERC721
                    .filter(token -> (checkCounter%2 == 0 || token.hasPositiveBalance() || token.isEthereum())) //only check zero balance tokens every other cycle
                    .concatMap(token -> fetchTokensInteract.updateDefaultBalance(token, defaultNetwork.getValue(), defaultWallet.getValue()))
                    .concatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokenBalanceUpdate, this::tkError, this::onFetchTokensBalanceCompletable);
        }
    }

    private void onTokenBalanceUpdate(Token token)
    {
        tokenUpdate.postValue(token);
        tokensService.addToken(token);
    }

    private void tkError(Throwable throwable)
    {
        Crashlytics.logException(throwable);
        throwable.printStackTrace();
        onError(throwable);
        //restart a refresh
        fetchTokens();
    }

    private void onFetchTokensBalanceCompletable()
    {
        checkCounter++;
        progress.postValue(false);
        balanceCheckDisposable = null;
        if (tokenCache != null && tokenCache.length > 0)
        {
            checkTokens.postValue(true);
        }
        else
        {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }

        if (!isVisible && balanceTimerDisposable != null)
        {
            balanceTimerDisposable.dispose();
            balanceTimerDisposable = null;
        }

        if (firstRun)
        {
            firstRun = false;
            //get the XML address
            setContractAddresses();
        }
    }
}
