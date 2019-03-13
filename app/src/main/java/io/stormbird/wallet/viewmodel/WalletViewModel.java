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
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.Erc20DetailRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.service.TokensService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final MutableLiveData<Boolean> tokensReady = new MutableLiveData<>();
    private final MutableLiveData<Integer> fetchKnownContracts = new MutableLiveData<>();

    private final MutableLiveData<String> checkAddr = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final Erc20DetailRouter erc20DetailRouter;
    private final AssetDisplayRouter assetDisplayRouter;
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
    private int checkCounter;

    private ConcurrentLinkedQueue<String> unknownAddresses;

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
            Erc20DetailRouter erc20DetailRouter,
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
        this.erc20DetailRouter = erc20DetailRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.openseaService = openseaService;
        this.tokensService = tokensService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        checkCounter = 0;
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
    public LiveData<Boolean> tokensReady() { return tokensReady; }
    public LiveData<Integer> fetchKnownContracts() { return fetchKnownContracts; }

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
        terminateBalanceUpdate();
    }

    private void terminateBalanceUpdate()
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable.dispose();
        }
        if (balanceCheckDisposable != null && !balanceCheckDisposable.isDisposed())
        {
            balanceCheckDisposable.dispose();
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
        if (!BuildConfig.DEBUG) Crashlytics.logException(throwable);
        throwable.printStackTrace();
        onError(throwable);
    }

    /**
     * Stage 2: Fetch opensea tokens
     */
    private void fetchFromOpensea()
    {
        List<Token> serviceList = tokensService.getAllLiveTokens();
        tokenCache = serviceList.toArray(new Token[0]);

        tokens.postValue(tokenCache);

        updateTokens = openseaService.getTokens(defaultWallet.getValue().address, defaultNetwork.getValue().chainId)
                //openseaService.getTokens("0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::gotOpenseaTokens, this::onOpenseaError);
    }

    private void gotOpenseaTokens(Token[] tokens)
    {
        //update the display list for token removals
        List<String> removedTokens = tokensService.getRemovedTokensOfClass(tokens, ERC721Token.class);

        if (!removedTokens.isEmpty())
        {
            removeTokens.postValue(removedTokens); //remove from UI
        }

        tokensService.clearBalanceOf(ERC721Token.class);
        tokensService.addTokens(tokens);

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
        //This is expected to happen - opensea gets a lot of activity
        //proceed using the stored data until we no longer get an error
        onFetchTokensCompletable();
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
        tokensReady.postValue(true);

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
        if (balanceTimerDisposable == null || balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable = Observable.interval(0, GET_BALANCE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> updateBalances()).subscribe();
        }
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
        addTokenRouter.open(context, null);
    }

    @Override
    public void showSendToken(Context context, String address, String symbol, int decimals, Token token) {
        boolean isToken = true;
        if (address.equalsIgnoreCase(defaultWallet().getValue().address)) isToken = false;
        sendTokenRouter.open(context, address, symbol, decimals, isToken, defaultWallet.getValue(), token, defaultNetwork.getValue().chainId);
    }

    @Override
    public void showErc20TokenDetail(Context context, String address, String symbol, int decimals, Token token) {
        boolean isToken = !address.equalsIgnoreCase(defaultWallet().getValue().address);
        boolean hasDefinition = assetDefinitionService.hasDefinition(address);
        erc20DetailRouter.open(context, address, symbol, decimals, isToken, defaultWallet.getValue(), token, hasDefinition);
    }

    @Override
    public void showRedeemToken(Context context, Token token) {
        assetDisplayRouter.open(context, token);
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
        if (unknownAddresses == null) unknownAddresses = new ConcurrentLinkedQueue<>();
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
            balanceCheckDisposable = null;
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
        if (isVisible)
        {
            //restart if required
            prepare();
        }
    }

    public void checkKnownContracts(List<String> extraAddresses)
    {
        List<Token> tokens = tokensService.getAllTokens();
        //Add all unterminated contracts that have null names
        for (Token t : tokens) if (t.tokenInfo.name == null && !t.isTerminated()) extraAddresses.add(t.getAddress());

        List<String> contracts = assetDefinitionService.getAllContracts(defaultNetwork.getValue().chainId);
        for (String addr : extraAddresses)
        {
            if (!contracts.contains(addr))
                contracts.add(addr.toLowerCase());
        }

        unknownAddresses.addAll(tokensService.reduceToUnknown(contracts));
    }

    private void checkUnknownAddresses()
    {
        String address = unknownAddresses.poll();
        if (address != null)
        {
            disposable = setupTokensInteract.addToken(address)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe(this::resolvedToken, this::onTokenAddError);
        }
    }

    private void resolvedToken(TokenInfo info)
    {
        disposable = fetchTransactionsInteract.queryInterfaceSpecForService(info)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.address), defaultWallet.getValue()))
                .flatMap(token -> addTokenInteract.addTokenFunctionData(token, assetDefinitionService))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::finishedImport, this::onTokenAddError);
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

    public Token getTokenFromService(Token token)
    {
        Token serviceToken = tokensService.getToken(token.getAddress());
        return (serviceToken != null) ? serviceToken : token;
    }

    public void refreshAssetDefinedTokens()
    {
        checkCounter = 0;
    }

    @Override
    public void run()
    {
        if (isVisible && defaultNetwork.getValue() != null && defaultWallet.getValue() != null)
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

            checkForTerminatedTokens();
        }
    }

    private void onTokenBalanceUpdate(Token token)
    {
        tokenUpdate.postValue(token);
        tokensService.addToken(token);
    }

    private void checkForTerminatedTokens()
    {
        if (tokensService.getTerminationReport())
        {
            refreshTokens.postValue(true);
        }
    }

    private void tkError(Throwable throwable)
    {
        if (!BuildConfig.DEBUG) Crashlytics.logException(throwable);
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

        if (checkCounter % 4 == 0)
        {
            fetchFromOpensea();
        }

        if (checkCounter == 1)
        {
            fetchKnownContracts.postValue(defaultNetwork.getValue().chainId);
        }
        checkUnknownAddresses();
    }

    public void resetAndFetchTokens()
    {
        checkCounter = 0;
        fetchTokens();
    }
}
