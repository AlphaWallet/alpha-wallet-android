package com.alphawallet.app.viewmodel;


import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.crashlytics.android.Crashlytics;
import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
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

import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.app.widget.InputAddressView.BARCODE_READER_REQUEST_CODE;

public class WalletViewModel extends BaseViewModel
{
    private static final int BALANCE_CHECK_INTERVAL_MILLIS = 500; //Balance check interval in milliseconds - should be integer divisible with 1000 (1 second)
    private static final int CHECK_OPENSEA_INTERVAL_TIME = 40; //Opensea refresh interval in seconds
    private static final int OPENSEA_RINKEBY_CHECK = 3; //check Rinkeby opensea once per XX opensea checks (ie if interval time is 25 and rinkeby check is 1 in 6, rinkeby refresh time is once per 300 seconds).
    public static double VALUE_THRESHOLD = 200.0; //$200 USD value is difference between red and grey backup warnings
    private static final int BALANCE_UPDATE_CORRECTION_FACTOR = 1000 / BALANCE_CHECK_INTERVAL_MILLIS;

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> tokensReady = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GenericWalletInteract.BackupLevel> backupEvent = new MutableLiveData<>();

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
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final MyAddressRouter myAddressRouter;

    private final MutableLiveData<Map<String, String>> currentWalletBalance = new MutableLiveData<>();

    private boolean isVisible = false;
    private int openSeaCheckCounter;
    private Wallet currentWallet;
    private int backupCheckVal;

    private ConcurrentLinkedQueue<ContractLocator> unknownAddresses;

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
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ChangeTokenEnableInteract changeTokenEnableInteract,
            MyAddressRouter myAddressRouter)
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
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.myAddressRouter = myAddressRouter;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }
    public LiveData<BigDecimal> total() {
        return total;
    }
    public LiveData<Token> tokenUpdate() { return tokenUpdate; }
    public LiveData<Boolean> tokensReady() { return tokensReady; }
    public LiveData<Wallet> defaultWallet() { return defaultWallet; }
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

    public void terminateBalanceUpdate()
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
            openSeaCheckCounter = CHECK_OPENSEA_INTERVAL_TIME * BALANCE_UPDATE_CORRECTION_FACTOR - 10; //schedule opensea check soon after refresh
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
        checkUnknownTokens(cachedTokens);
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
        updateTokenBalances();
        assetDefinitionService.checkTokenscriptEnabledTokens(tokensService);
        assetDefinitionService.getAllLoadedScripts() //holds for loading complete then returns origin contracts
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::addUnresolvedContracts)
                .isDisposed();
    }

    /**
     * Stage 2: Fetch opensea tokens
     */
    private void fetchFromOpensea(NetworkInfo checkNetwork)
    {
        if (checkNetwork == null) return;
        Log.d("OPENSEA", "Fetch from opensea : " + checkNetwork.getShortName());
        updateTokens = openseaService.getTokens(currentWallet.address, checkNetwork.chainId, checkNetwork.getShortName())
                .flatMap(tokens -> fetchTokensInteract.checkInterface(tokens, currentWallet))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokens -> gotOpenseaTokens(checkNetwork.chainId, tokens), this::onOpenseaError);
    }

    private void gotOpenseaTokens(int chainId, Token[] openSeaTokens)
    {
        if (openSeaTokens.length == 1 && openSeaTokens[0].tokenInfo == null)
        {
            return; //early return if long opensea call still in progress (can happen with wallets containing very large numbers of ERC721 tokens).
        }

        ContractType[] filterTypes = { ContractType.ERC721, ContractType.ERC721_LEGACY, ContractType.ERC721_TICKET, ContractType.ERC721_UNDETERMINED };
        List<Token> erc721Tokens = tokensService.getChangedTokenBalance(chainId, openSeaTokens, filterTypes);
        tokens.postValue(tokensService.getAllTokens(filterTypes)); //update ERC721 types using balances from appropriate source. ERC721 Ticket balance fetch from opensea is usually wrong

        if (erc721Tokens.size() > 0)
        {
            //store these tokens
            updateTokens = addTokenInteract.storeTokens(currentWallet, erc721Tokens.toArray(new Token[0]))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::storedTokens, this::onError);
        }

        //now update network tokens
        if (chainId == MAINNET_ID) getTokensOnNetwork();
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

    private void getTokensOnNetwork()
    {
        ethereumNetworkRepository.getTokensOnNetwork(MAINNET_ID, currentWallet.address, tokensService)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::receiveNetworkTokens, this::onTokenBalanceError).isDisposed();
    }

    private void receiveNetworkTokens(Token[] receivedTokens)
    {
        Token[] updatedTokens = tokensService.addTokens(receivedTokens); 
        //add these tokens to the display
        tokens.postValue(updatedTokens); //Note: return from addTokens filters out the ContractType.OTHER tokens

        for (Token t : receivedTokens) //Now add unrecognised tokens to scan list
        {
            if (t.getInterfaceSpec() == ContractType.OTHER)
            {
                unknownAddresses.add(new ContractLocator(t.getAddress(), t.tokenInfo.chainId));
            }
        }

        //now store the updated tokens
        if (updatedTokens.length > 0)
        {
            addTokenInteract.addERC20(currentWallet, updatedTokens)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::storedTokens, this::onError).isDisposed();
        }
    }

    private void onTokenBalanceError(Throwable throwable)
    {
        //unable to resolve - phone may be offline
    }

    private void onFetchTokensCompletable()
    {
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
        progress.postValue(false);
        addUnresolvedContracts(ethereumNetworkRepository.getAllKnownContracts(tokensService.getNetworkFilters()));
        if (balanceTimerDisposable == null || balanceTimerDisposable.isDisposed())
        {
            balanceTimerDisposable = Observable.interval(0, BALANCE_CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> checkBalances()).subscribe();
        }
    }

    private void addUnresolvedContracts(List<ContractLocator> contractCandidates)
    {
        if (contractCandidates != null && contractCandidates.size() > 0)
        {
            Observable.fromArray(contractCandidates.toArray(new ContractLocator[0]))
                    .filter(result -> (tokensService.getToken(result.chainId, result.address) == null))
                    .forEach(r -> unknownAddresses.add(r)).isDisposed();
        }
    }

    /**
     * Checks for tokens with contract type 'OTHER' returned from the initial storage fetch
     * These tokens will have their type interface re-checked.
     */
    private void checkUnknownTokens(Token[] tokens)
    {
        for (Token t : tokens)
        {
            if (t.getInterfaceSpec() == ContractType.OTHER)
            {
                unknownAddresses.add(new ContractLocator(t.getAddress(), t.tokenInfo.chainId));
            }
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
        checkUnknownAddresses();
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

    public void showAddToken(Context context) {
        addTokenRouter.open(context, null);
    }

    public void showQRCodeScanning(Activity activity) {
        Intent intent = new Intent(activity, QRScanningActivity.class);
        intent.putExtra(C.EXTRA_UNIVERSAL_SCAN, true);
        activity.startActivityForResult(intent, C.REQUEST_UNIVERSAL_SCAN);
    }

    @Override
    public void showErc20TokenDetail(Context context, @NotNull String address, String symbol, int decimals, @NotNull Token token) {
        boolean isToken = !address.equalsIgnoreCase(currentWallet.address);
        boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, address);
        erc20DetailRouter.open(context, address, symbol, decimals, isToken, currentWallet, token, hasDefinition);
    }

    @Override
    public void showTokenList(Context context, Token token) {
        assetDisplayRouter.open(context, token, currentWallet);
    }

    public LiveData<Map<String, String>> currentWalletBalance() {
        return currentWalletBalance;
    }

    public void prepare()
    {
        if (unknownAddresses == null) unknownAddresses = new ConcurrentLinkedQueue<>();
        if (currentWallet == null)
        {
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
        defaultWallet.postValue(wallet);
        fetchTokens();
    }

    public void setVisibility(boolean visibility) {
        isVisible = visibility;
    }

    private void checkUnknownAddresses()
    {
        ContractLocator contract = unknownAddresses.poll();

        if (contract != null)
        {
            disposable = setupTokensInteract.addToken(contract.address, contract.chainId) //fetch tokenInfo
                    .filter(tokenInfo -> tokenInfo.name != null)
                    .flatMap(tokenInfo -> fetchTransactionsInteract.queryInterfaceSpec(tokenInfo).toObservable()
                            .flatMap(contractType -> addTokenInteract.add(tokenInfo, contractType, currentWallet)))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(this::finishedImport, this::onError);
        }
    }

    private void finishedImport(Token token)
    {
        tokensService.addToken(token);
        if (EthereumNetworkRepository.isPriorityToken(token)) tokenUpdate.postValue(token);
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

    public Token getTokenFromService(int chainId, String addr)
    {
        return tokensService.getToken(chainId, addr);
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
                    .subscribe(backupEvent::postValue, this::onTokenBalanceError).isDisposed();
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
                    .flatMap(tokens -> fetchTokensInteract.storeTickers(currentWallet, tokens)) //store tickers so they can be recreated at startup. This stops the token display from 'janking' around at startup
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
            openSeaCheckCounter += 2;
            if ((openSeaCheckCounter % 2) == 1)
            {
                openSeaCheckCounter++;
            }
        }
        else
        {
            openSeaCheckCounter ++;
        }

        if (openSeaCheckCounter == backupCheckVal) checkBackup();

        if (openSeaCheckCounter % (CHECK_OPENSEA_INTERVAL_TIME * BALANCE_UPDATE_CORRECTION_FACTOR) == 0)
        {
            NetworkInfo openSeaCheck = ethereumNetworkRepository.getNetworkByChain(MAINNET_ID);

            if (openSeaCheckCounter % (CHECK_OPENSEA_INTERVAL_TIME * BALANCE_UPDATE_CORRECTION_FACTOR * OPENSEA_RINKEBY_CHECK) == 0 && ethereumNetworkRepository.getFilterNetworkList().contains(EthereumNetworkRepository.RINKEBY_ID))
            {
                openSeaCheck = ethereumNetworkRepository.getNetworkByChain(EthereumNetworkRepository.RINKEBY_ID);
            }

            fetchFromOpensea(openSeaCheck);
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

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public void setTokenEnabled(Token token, boolean enabled) {
        changeTokenEnableInteract.setEnable(currentWallet, token, enabled);
        token.tokenInfo.isEnabled = enabled;
    }

    public void newTokensFound(List<ContractLocator> tokenContracts)
    {
        addUnresolvedContracts(tokenContracts);
    }

    public void showMyAddress(Context context)
    {
        myAddressRouter.open(context, currentWallet);
    }
}
