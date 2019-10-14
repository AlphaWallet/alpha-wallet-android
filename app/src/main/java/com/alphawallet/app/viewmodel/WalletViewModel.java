package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.crashlytics.android.Crashlytics;
import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ERC721Token;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.router.AddTokenRouter;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.Erc20DetailRouter;
import com.alphawallet.app.router.SendTokenRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.service.TokensService;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class WalletViewModel extends BaseViewModel
{
    private static final int BALANCE_CHECK_INTERVAL_MILLIS = 500; //Balance check interval in milliseconds - should be integer divisible with 1000 (1 second)
    private static final int CHECK_OPENSEA_INTERVAL_TIME = 40; //Opensea refresh interval in seconds
    private static final int CHECK_BLOCKSCOUT_INTERVAL_TIME = 30;
    private static final int OPENSEA_RINKEBY_CHECK = 3; //check Rinkeby opensea once per XX opensea checks (ie if interval time is 25 and rinkeby check is 1 in 6, rinkeby refresh time is once per 300 seconds).
    public static double VALUE_THRESHOLD = 200.0; //$200 USD value is difference between red and grey backup warnings

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> tokensReady = new MutableLiveData<>();
    private final MutableLiveData<Boolean> fetchKnownContracts = new MutableLiveData<>();
    private final MutableLiveData<GenericWalletInteract.BackupLevel> backupEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> checkAdapterCount = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final Erc20DetailRouter erc20DetailRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final OpenseaService openseaService;
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    private final MutableLiveData<Map<String, String>> currentWalletBalance = new MutableLiveData<>();

    private boolean isVisible = false;
    private int openSeaCheckCounter;
    private Wallet currentWallet;
    private int backupCheckVal;

    private ConcurrentLinkedQueue<ContractResult> unknownAddresses;

    @Nullable
    private Disposable balanceTimerDisposable;
    @Nullable
    private Disposable updateTokens;
    @Nullable
    private Disposable balanceCheckDisposable;

    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            Erc20DetailRouter erc20DetailRouter,
            AssetDisplayRouter assetDisplayRouter,
            GenericWalletInteract genericWalletInteract,
            AddTokenInteract addTokenInteract,
            SetupTokensInteract setupTokensInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            OpenseaService openseaService,
            FetchTransactionsInteract fetchTransactionsInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository)
    {
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.erc20DetailRouter = erc20DetailRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.genericWalletInteract = genericWalletInteract;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.openseaService = openseaService;
        this.tokensService = tokensService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }
    public LiveData<BigDecimal> total() {
        return total;
    }
    public LiveData<Token> tokenUpdate() { return tokenUpdate; }
    public LiveData<Boolean> tokensReady() { return tokensReady; }
    public LiveData<Boolean> checkAdapterCount() { return checkAdapterCount; }
    public LiveData<Boolean> fetchKnownContracts() { return fetchKnownContracts; }
    public LiveData<GenericWalletInteract.BackupLevel> backupEvent() { return backupEvent; }

    public String getWalletAddr() { return currentWallet != null ? currentWallet.address : null; }
    public WalletType getWalletType() { return currentWallet != null ? currentWallet.type : WalletType.KEYSTORE; }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    //we changed wallets or network, ensure we clean up before displaying new data
    public void clearProcess()
    {
        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
        }
        terminateBalanceUpdate();
        tokensService.clearTokens();
    }

    private void terminateBalanceUpdate()
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable.dispose();
            balanceTimerDisposable = null;
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
        if (currentWallet != null)
        {
            openSeaCheckCounter = 0;
            backupCheckVal = 0;
            tokensService.setCurrentAddress(currentWallet.address);
            updateTokens = fetchTokensInteract.fetchStoredWithEth(currentWallet)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokens, this::onTokenFetchError, this::startBalanceUpdate);
        }
        else
        {
            //called fetch tokens but don't have any wallet yet - view is not prepared
            prepare();
        }
    }

    private void onTokens(Token[] cachedTokens)
    {
        if (cachedTokens.length == 0) //require another reset
        {
            currentWallet = null;
            prepare();
        }
        tokensService.addTokens(cachedTokens);
        tokensService.requireTokensRefresh();
        tokens.postValue(tokensService.getAllLiveTokens().toArray(new Token[0]));
        fetchKnownContracts.postValue(true);
    }

    public List<Integer> getChainFilters()
    {
        return tokensService.getNetworkFilters();
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

    private void startBalanceUpdate()
    {
        fetchFromOpensea(ethereumNetworkRepository.getNetworkByChain(EthereumNetworkRepository.MAINNET_ID));
        updateTokenBalances();
        assetDefinitionService.checkTokenscriptEnabledTokens(tokensService);
    }

    /**
     * Stage 2: Fetch opensea tokens
     */
    private void fetchFromOpensea(NetworkInfo checkNetwork)
    {
        Log.d("OPENSEA", "Fetch from opensea : " + checkNetwork.getShortName());
        updateTokens = openseaService.getTokens(currentWallet.address, checkNetwork.chainId, checkNetwork.getShortName())
                //openseaService.getTokens("0xbc8dAfeacA658Ae0857C80D8Aa6dE4D487577c63")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokens -> gotOpenseaTokens(checkNetwork.chainId, tokens), this::onOpenseaError);
    }

    private void gotOpenseaTokens(int chainId, Token[] openSeaTokens)
    {
        //zero out balance of tokens
        Token[] erc721Tokens = tokensService.zeroiseBalanceOfSpentTokens(chainId, openSeaTokens, ERC721Token.class);

        if (erc721Tokens.length > 0)
        {
            tokensService.addTokens(erc721Tokens);

            tokens.postValue(erc721Tokens);

            //store these tokens
            updateTokens = addTokenInteract.addERC721(currentWallet, erc721Tokens)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::storedTokens, this::onError);
        }

        progress.postValue(false);
    }

    private void onOpenseaError(Throwable throwable)
    {
        //This is expected to happen - opensea gets a lot of activity
        //proceed using the stored data until we no longer get an error
        onFetchTokensCompletable();
    }

    private void storedTokens(Token[] tokens)
    {
        onFetchTokensCompletable();
    }

    private void fetchFromBlockscout()
    {
        updateTokens = tokensService.getTokensAtAddress()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::receiveNetworkTokens, this::onBlockscoutError);
    }

    private void receiveNetworkTokens(Token[] receivedTokens)
    {
        //add these tokens to the display
        tokens.postValue(receivedTokens);
        Token[] updatedTokens = tokensService.addTokens(receivedTokens);

        //now store the updated tokens
        if (updatedTokens.length > 0)
        {
            addTokenInteract.addERC20(currentWallet, updatedTokens)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::storedTokens, this::onError).isDisposed();

            //TODO: Once we start receiving ERC20 tickers check backup requirement if ERC20 received
            //checkBackup();
        }
    }

    private void onBlockscoutError(Throwable throwable)
    {
        //unable to resolve blockscout - phone may be offline
    }

    private void onFetchTokensCompletable()
    {
        progress.postValue(false);

        tokensReady.postValue(true);

        if (updateTokens != null && !updateTokens.isDisposed())
        {
            updateTokens.dispose();
            updateTokens = null;
        }
    }

    /**
     * Start the token checking thread
     */
    private void updateTokenBalances()
    {
        VisibilityFilter.addPriorityTokens(unknownAddresses, tokensService);
        checkUnknownAddresses(true);
        if (balanceTimerDisposable == null || balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable = Observable.interval(0, BALANCE_CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> checkBalances()).subscribe();
        }
    }

    private void checkBalances()
    {
        // This checks for an old thread running. Terminate if it is. Possibly destroy the fragment and re-create on wallet change
        if (!currentWallet.address.equalsIgnoreCase(tokensService.getCurrentAddress())
                && balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable.dispose();
            balanceTimerDisposable = null;
        }

        checkTokenUpdates();
        checkUnknownAddresses(false);
        checkOpenSeaUpdate();
        checkTickers();
    }

    private void checkTokenUpdates()
    {
        Token t = tokensService.getNextInBalanceUpdateQueue();

        if (t != null)
        {
            Log.d("TOKEN", "Updating: " + t.tokenInfo.name + " : " + t.getAddress() + " [" + t.balanceUpdateWeight + "]");
            balanceCheckDisposable = fetchTokensInteract.updateDefaultBalance(t, currentWallet)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokenUpdate, this::balanceUpdateError, this::checkComplete);
        }
    }

    private void checkComplete()
    {
        balanceCheckDisposable = null;
        checkUIUpdates();
    }

    private void balanceUpdateError(Throwable throwable)
    {
        balanceCheckDisposable = null;
    }

    private void checkUIUpdates()
    {
        disposable = Observable.fromCallable(tokensService::getAllTokens)
                .flatMapIterable(token -> token)
                .filter(Token::walletUIUpdateRequired)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokenUpdate::postValue, this::onError);
    }

    private void onTokenUpdate(Token token)
    {
        if (backupCheckVal == 0 && token != null && token.hasRealValue() && token.isEthereum() && token.ticker != null)
        {
            backupCheckVal = openSeaCheckCounter + 5;
        }
        balanceCheckDisposable = null;
        if (token == null) return;
        Token update = tokensService.addToken(token);
        if (update != null)
        {
            tokenUpdate.postValue(update);
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
    public void showErc20TokenDetail(Context context, @NotNull String address, String symbol, int decimals, @NotNull Token token) {
        boolean isToken = !address.equalsIgnoreCase(currentWallet.address);
        boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, address);
        erc20DetailRouter.open(context, address, symbol, decimals, isToken, currentWallet, token, hasDefinition);
    }

    @Override
    public void showRedeemToken(Context context, Token token) {
        assetDisplayRouter.open(context, token);
    }

    public LiveData<Map<String, String>> currentWalletBalance() {
        return currentWalletBalance;
    }

    public void prepare()
    {
        if (unknownAddresses == null) unknownAddresses = new ConcurrentLinkedQueue<>();
        if (currentWallet == null)
        {
            progress.postValue(true);
            disposable = genericWalletInteract
                    .find()
                    .subscribe(this::onDefaultWallet, this::onError);
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

    private void onDefaultWallet(@NotNull Wallet wallet) {
        tokensService.setCurrentAddress(wallet.address);
        currentWallet = wallet;
        fetchTokens();
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }

    public void checkKnownContracts(List<ContractResult> knownContracts)
    {
        List<Token> tokens = tokensService.getAllTokens();
        //Add all unterminated contracts that have null names
        for (Token t : tokens) if (t.tokenInfo.name == null && !t.isTerminated()) ContractResult.addIfNotInList(knownContracts, new ContractResult(t.getAddress(), t.tokenInfo.chainId));

        for (NetworkInfo network : ethereumNetworkRepository.getAvailableNetworkList())
        {
            List<String> contracts = assetDefinitionService.getAllContracts(network.chainId);
            for (String contract : contracts)
            {
                ContractResult test = new ContractResult(contract, network.chainId);
                ContractResult.addIfNotInList(knownContracts, test);
            }
        }

        unknownAddresses.addAll(tokensService.reduceToUnknown(knownContracts));
    }

    private void checkUnknownAddresses(boolean refresh)
    {
        ContractResult contract = unknownAddresses.poll();
        if (contract != null)
        {
            disposable = setupTokensInteract.addToken(contract.name, contract.chainId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .subscribe(tokenInfo -> resolvedToken(tokenInfo, refresh), this::onTokenAddError);
        }
    }

    private void resolvedToken(TokenInfo info, boolean refresh)
    {
        disposable = fetchTransactionsInteract.queryInterfaceSpecForService(info)
                .flatMap(tokenInfo -> addTokenInteract.add(tokenInfo, tokensService.getInterfaceSpec(tokenInfo.chainId, tokenInfo.address), currentWallet))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(token -> finishedImport(token, refresh), this::onTokenAddError);
    }

    private void finishedImport(Token token, boolean refresh)
    {
        tokensService.addToken(token);
        if (refresh) tokenUpdate.postValue(token);
    }

    private void onTokenAddError(Throwable throwable)
    {
        //cannot add the token until we get internet connection
        Log.d("WVM", "Wait for internet");
    }

    public Token getTokenFromService(@NotNull Token token)
    {
        Token serviceToken = tokensService.getToken(token.tokenInfo.chainId, token.getAddress());
        return (serviceToken != null) ? serviceToken : token;
    }

    private void tkError(Throwable throwable)
    {
        if (!BuildConfig.DEBUG) Crashlytics.logException(throwable);
        throwable.printStackTrace();
        onError(throwable);
        //restart a refresh
        fetchTokens();
    }

    public void resetAndFetchTokens()
    {
        fetchTokens();
    }

    private void checkBackup()
    {
        if (getWalletAddr() == null) return;
        BigDecimal value = BigDecimal.ZERO;
        //first see if wallet has any value
        for (Token t : tokensService.getAllTokens())
        {
            if (t.hasRealValue() && t.ticker != null && t.hasPositiveBalance())
            {
                BigDecimal balance = t.balance.divide(new BigDecimal(Math.pow(10, t.tokenInfo.decimals)));
                value = value.add(balance.multiply(new BigDecimal(t.ticker.price)));
            }
        }

        if (value.compareTo(BigDecimal.ZERO) > 0)
        {
            final BigDecimal calcValue = value;
            genericWalletInteract.getBackupWarning(getWalletAddr())
                    .map(needsBackup -> calculateBackupWarning(needsBackup, calcValue))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(backupEvent::postValue, this::onBlockscoutError).isDisposed();
        }
    }

    private GenericWalletInteract.BackupLevel calculateBackupWarning(Boolean needsBackup, @NotNull BigDecimal value)
    {
        if (!needsBackup)
        {
            return GenericWalletInteract.BackupLevel.BACKUP_NOT_REQUIRED;
        }
        else if (value.compareTo(BigDecimal.valueOf(VALUE_THRESHOLD)) >= 0)
        {
            return GenericWalletInteract.BackupLevel.WALLET_HAS_HIGH_VALUE;
        }
        else
        {
            return GenericWalletInteract.BackupLevel.WALLET_HAS_LOW_VALUE;
        }
    }

    private void checkTickers()
    {
        if (ethereumNetworkRepository.checkTickers())
        {
            ethereumNetworkRepository.attachTokenTickers(tokensService.getAllLiveTokens().toArray(new Token[0]))
                    .observeOn(Schedulers.newThread())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(tokens::postValue).isDisposed();
        }
    }

    /**
     * Check if we need to update opensea: See params in class header
     */
    private void checkOpenSeaUpdate()
    {
        if (isVisible) //update at half speed if not visible
        {
            openSeaCheckCounter++;
        }
        else
        {
            openSeaCheckCounter += 2;
        }

        //init events
        switch (openSeaCheckCounter)
        {
            case 4:
                if (ethereumNetworkRepository.getFilterNetworkList().contains(EthereumNetworkRepository.RINKEBY_ID))
                    fetchFromOpensea(ethereumNetworkRepository.getNetworkByChain(EthereumNetworkRepository.RINKEBY_ID));
                break;
            default:
                break;
        }

        if (openSeaCheckCounter == backupCheckVal) checkBackup();

        int updateCorrection = 1000 / BALANCE_CHECK_INTERVAL_MILLIS;

        if (openSeaCheckCounter % (CHECK_OPENSEA_INTERVAL_TIME * updateCorrection) == 0)
        {
            NetworkInfo openSeaCheck = ethereumNetworkRepository.getNetworkByChain(EthereumNetworkRepository.MAINNET_ID);

            if (openSeaCheckCounter % (CHECK_OPENSEA_INTERVAL_TIME * updateCorrection * OPENSEA_RINKEBY_CHECK) == 0 && ethereumNetworkRepository.getFilterNetworkList().contains(EthereumNetworkRepository.RINKEBY_ID))
            {
                openSeaCheck = ethereumNetworkRepository.getNetworkByChain(EthereumNetworkRepository.RINKEBY_ID);
            }

            fetchFromOpensea(openSeaCheck);
        }
        else if ((openSeaCheckCounter - 8) % (CHECK_BLOCKSCOUT_INTERVAL_TIME * updateCorrection) == 0)
        {
            fetchFromBlockscout();
        }
    }

    public Disposable setKeyBackupTime(String walletAddr)
    {
        return genericWalletInteract.updateBackupTime(walletAddr);
    }

    public Disposable setKeyWarningDismissTime(String walletAddr)
    {
        return genericWalletInteract.updateWarningTime(walletAddr);
    }

    public Wallet getWallet()
    {
        return currentWallet;
    }
}
