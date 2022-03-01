package com.alphawallet.app.service;

import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.ServiceSyncCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ContractAddress;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import timber.log.Timber;

public class TokensService
{
    private static final String TAG = "TOKENSSERVICE";
    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";
    public static final long PENDING_TIME_LIMIT = 3*DateUtils.MINUTE_IN_MILLIS; //cut off pending chain after 3 minutes

    private static final Map<Long, Long> pendingChainMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Token> tokenStoreList = new ConcurrentLinkedDeque<>(); //used to hold tokens that will be stored
    private final Map<String, Long> pendingTokenMap = new ConcurrentHashMap<>(); //used to determine which token to update next
    private String currentAddress = null;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;
    private final TickerService tickerService;
    private final OpenSeaService openseaService;
    private final AnalyticsServiceType<AnalyticsProperties> analyticsService;
    private final List<Long> networkFilter;
    private ContractLocator focusToken;
    private final ConcurrentLinkedDeque<ContractAddress> unknownTokens;
    private final ConcurrentLinkedQueue<Long> baseTokenCheck;
    private long openSeaCheckId;
    private boolean appHasFocus;
    private boolean mainNetActive = true;
    private static boolean walletStartup = false;
    private long transferCheckChain;
    private final TokenFactory tokenFactory = new TokenFactory();
    private long syncTimer;
    private long syncStart;
    private ServiceSyncCallback completionCallback;
    private int syncCount = 0;

    @Nullable
    private Disposable eventTimer;
    @Nullable
    private Disposable checkUnknownTokenCycle;
    @Nullable
    private Disposable queryUnknownTokensDisposable;
    @Nullable
    private Disposable balanceCheckDisposable;
    @Nullable
    private Disposable erc20CheckDisposable;
    @Nullable
    private Disposable tokenStoreDisposable;
    @Nullable
    private Disposable openSeaQueryDisposable;

    private static boolean done = false;

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository,
                         TokenRepositoryType tokenRepository,
                         TickerService tickerService,
                         OpenSeaService openseaService,
                         AnalyticsServiceType<AnalyticsProperties> analyticsService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
        this.tickerService = tickerService;
        this.openseaService = openseaService;
        this.analyticsService = analyticsService;
        networkFilter = new ArrayList<>();
        setupFilter(ethereumNetworkRepository.hasSetNetworkFilters());
        focusToken = null;
        this.unknownTokens = new ConcurrentLinkedDeque<>();
        this.baseTokenCheck = new ConcurrentLinkedQueue<>();
        setCurrentAddress(ethereumNetworkRepository.getCurrentWalletAddress()); //set current wallet address at service startup
        appHasFocus = true;
        transferCheckChain = 0;
        completionCallback = null;
    }

    private void checkUnknownTokens()
    {
        if (queryUnknownTokensDisposable == null || queryUnknownTokensDisposable.isDisposed())
        {
            ContractAddress t = unknownTokens.pollFirst();
            Token cachedToken = t != null ? getToken(t.chainId, t.address) : null;

            if (t != null && t.address.length() > 0 && (cachedToken == null || TextUtils.isEmpty(cachedToken.tokenInfo.name)))
            {
                queryUnknownTokensDisposable = tokenRepository.update(t.address, t.chainId).toObservable() //fetch tokenInfo
                        .filter(tokenInfo -> (!TextUtils.isEmpty(tokenInfo.name) || !TextUtils.isEmpty(tokenInfo.symbol)) && tokenInfo.chainId != 0)
                        .map(tokenInfo -> { tokenInfo.isEnabled = false; return tokenInfo; }) //set default visibility to false
                        .flatMap(tokenInfo -> tokenRepository.determineCommonType(tokenInfo).toObservable()
                            .map(contractType -> tokenFactory.createToken(tokenInfo, contractType, ethereumNetworkRepository.getNetworkByChain(t.chainId).getShortName())))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(this::finishAddToken, err -> onCheckError(err, t), this::finishTokenCheck);
            }
            else if (t == null)
            {
                //stop the check
                if (checkUnknownTokenCycle != null && !checkUnknownTokenCycle.isDisposed())
                    checkUnknownTokenCycle.dispose();
            }
        }
    }

    private void onCheckError(Throwable throwable, ContractAddress t)
    {
        Timber.e(throwable);
    }

    private void finishTokenCheck()
    {
        queryUnknownTokensDisposable = null;
    }

    private void finishAddToken(Token token)
    {
        if (token != null && token.getInterfaceSpec() != ContractType.OTHER)
        {
            tokenStoreList.add(token);
        }
    }

    public Token getToken(long chainId, String addr)
    {
        if (TextUtils.isEmpty(currentAddress) || TextUtils.isEmpty(addr)) return null;
        else return tokenRepository.fetchToken(chainId, currentAddress, addr.toLowerCase());
    }

    public void storeToken(Token token)
    {
        if (TextUtils.isEmpty(currentAddress) || token == null || token.getInterfaceSpec() == ContractType.OTHER) return;
        tokenStoreDisposable = tokenRepository.checkInterface(new Token[] { token }, new Wallet(token.getWallet()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tkn -> Collections.addAll(tokenStoreList, tkn), this::onERC20Error);
    }

    public TokenTicker getTokenTicker(Token token)
    {
        return tokenRepository.getTokenTicker(token);
    }

    public Single<TokenCardMeta[]> getAllTokenMetas(String searchString)
    {
        return tokenRepository.fetchAllTokenMetas(new Wallet(currentAddress), networkFilter, searchString);
    }

    public List<Token> getAllAtAddress(String addr)
    {
        List<Token> tokens = new ArrayList<>();
        if (addr == null) return tokens;
        for (long chainId : networkFilter)
        {
            tokens.add(getToken(chainId, addr));
        }

        return tokens;
    }

    public void setCurrentAddress(String newWalletAddr)
    {
        if (newWalletAddr != null && (currentAddress == null || !currentAddress.equalsIgnoreCase(newWalletAddr)))
        {
            currentAddress = newWalletAddr.toLowerCase();
            stopUpdateCycle();
            addLockedTokens();
            if (openseaService != null) openseaService.resetOffsetRead(networkFilter);
        }
    }

    private void updateCycle(boolean val)
    {
        syncStart = System.currentTimeMillis();
        syncTimer = syncStart + 5*DateUtils.SECOND_IN_MILLIS;

        eventTimer = Observable.interval(1, 500, TimeUnit.MILLISECONDS)
                .doOnNext(l -> checkTokensBalance())
                .observeOn(Schedulers.newThread()).subscribe();
    }

    public void startUpdateCycle()
    {
        stopUpdateCycle();
        if (!Utils.isAddressValid(currentAddress)) return;

        syncCount = 0;

        setupFilters();

        eventTimer = Single.fromCallable(() -> {
            startupPass();
            addUnresolvedContracts(ethereumNetworkRepository.getAllKnownContracts(getNetworkFilters()));
            checkIssueTokens();
            pendingTokenMap.clear();
            return true;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCycle, this::onError);
    }

    // Constructs a map of tokens requiring update
    private TokenCardMeta[] buildUpdateMap()
    {
        int unSynced = 0;
        TokenCardMeta[] tokenList = tokenRepository.fetchTokenMetasForUpdate(new Wallet(currentAddress), networkFilter);
        for (TokenCardMeta meta : tokenList)
        {
            meta.lastTxUpdate = meta.lastUpdate;
            String key = databaseKey(meta.getChain(), meta.getAddress());
            if (!pendingTokenMap.containsKey(key))
            {
                if (meta.type == ContractType.ERC20 || meta.type == ContractType.ETHEREUM) unSynced++;
                pendingTokenMap.put(key, meta.lastUpdate);
            }
            else if (meta.lastUpdate <= pendingTokenMap.get(key))
            {
                meta.lastUpdate = pendingTokenMap.get(key);
                if ((meta.type == ContractType.ERC20 || meta.type == ContractType.ETHEREUM)
                        && meta.lastUpdate < syncStart && meta.isEnabled && meta.hasValidName()) { unSynced++; }
            }
            else if ((meta.type == ContractType.ERC20 || meta.type == ContractType.ETHEREUM)
                    && meta.lastUpdate < syncStart && meta.isEnabled && meta.hasValidName()) { unSynced++; }
        }

        checkSyncStatus(unSynced, tokenList);

        return tokenList;
    }

    private void checkSyncStatus(int unSynced, TokenCardMeta[] tokenList)
    {
        if (syncTimer > 0 && System.currentTimeMillis() > syncTimer)
        {
            if (unSynced > 0)
            {
                syncTimer = System.currentTimeMillis() + 5*DateUtils.SECOND_IN_MILLIS;
            }
            else
            {
                syncTimer = 0;
                //sync chain tickers
            }

            syncChainTickers(tokenList, 0);
        }
    }

    private boolean syncERC20Tickers(final int chainIndex, final long chainId, final TokenCardMeta[] tokenList)
    {
        List<TokenCardMeta> erc20OnChain = getERC20OnChain(chainId, tokenList);
        if (erc20OnChain.size() > 0)
        {
            tickerService.syncERC20Tickers(chainId, erc20OnChain)
                    .subscribeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(v -> syncChainTickers(tokenList, chainIndex+1), this::onERC20Error)
                    .isDisposed();
            return true;
        }
        else
        {
            return false;
        }
    }

    private List<TokenCardMeta> getERC20OnChain( long chainId, TokenCardMeta[] tokenList)
    {
        List<TokenCardMeta> allERC20 = new ArrayList<>();
        for (TokenCardMeta tcm : tokenList)
        {
            if (tcm.type == ContractType.ERC20 && tcm.hasPositiveBalance() && tcm.getChain() == chainId)
            {
                allERC20.add(tcm);
            }
        }
        return allERC20;
    }

    private void syncChainTickers(TokenCardMeta[] tokenList, int chainIndex)
    {
        if (mainNetActive)
        {
            //go through all mainnet chains
            NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();

            for (int i = chainIndex; i < networks.length; i++)
            {
                NetworkInfo info = networks[i];
                if (info.hasRealValue() && syncERC20Tickers(i, info.chainId, tokenList)) return;
            }
        }

        //complete
        if (completionCallback != null)
        {
            completionCallback.syncComplete(this, syncCount);
        }
    }

    /**
     * Creates and stores an enabled basechain Token
     * @param chainId
     * @return
     */
    public Single<Token[]> createBaseToken(long chainId)
    {
        Token[] tok = new Token[1];
        tok[0] = tokenRepository.fetchToken(chainId, currentAddress, currentAddress);
        if (!networkFilter.contains(chainId)) //add chain to filter list
        {
            networkFilter.add(chainId);
            ethereumNetworkRepository.setFilterNetworkList(networkFilter.toArray(new Long[0]));
        }

        return tokenRepository.storeTokens(new Wallet(currentAddress), tok);
    }

    public void stopUpdateCycle()
    {
        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
            eventTimer = null;
        }

        if (balanceCheckDisposable != null && !balanceCheckDisposable.isDisposed()) { balanceCheckDisposable.dispose(); }
        if (erc20CheckDisposable != null && !erc20CheckDisposable.isDisposed()) { erc20CheckDisposable.dispose(); }
        if (tokenStoreDisposable != null && !tokenStoreDisposable.isDisposed()) { tokenStoreDisposable.dispose(); }
        if (openSeaQueryDisposable != null && !openSeaQueryDisposable.isDisposed()) { openSeaQueryDisposable.dispose(); }
        if (checkUnknownTokenCycle != null && !checkUnknownTokenCycle.isDisposed()) { checkUnknownTokenCycle.dispose(); }
        if (queryUnknownTokensDisposable != null && !queryUnknownTokensDisposable.isDisposed()) { queryUnknownTokensDisposable.dispose(); }
        if (openSeaQueryDisposable != null && !openSeaQueryDisposable.isDisposed()) { openSeaQueryDisposable.dispose(); }

        IconItem.resetCheck();
        pendingChainMap.clear();
        tokenStoreList.clear();
        baseTokenCheck.clear();
        pendingTokenMap.clear();
        unknownTokens.clear();
    }

    public String getCurrentAddress() { return currentAddress; }

    public static void setWalletStartup() { walletStartup = true; }

    public void setupFilter(boolean userUpdated)
    {
        networkFilter.clear();
        if (CustomViewSettings.getLockedChains().size() > 0)
        {
            networkFilter.addAll(CustomViewSettings.getLockedChains());
        }
        else
        {
            networkFilter.addAll(ethereumNetworkRepository.getFilterNetworkList());
        }

        if (userUpdated) ethereumNetworkRepository.setHasSetNetworkFilters();
    }

    public void setFocusToken(@NotNull Token token)
    {
        focusToken = new ContractLocator(token.getAddress(), token.tokenInfo.chainId);
    }

    public void clearFocusToken()
    {
        focusToken = null;
    }

    public void onWalletRefreshSwipe()
    {
        openseaService.resetOffsetRead(networkFilter);
    }

    private boolean isFocusToken(Token t)
    {
        return focusToken != null && focusToken.equals(t);
    }

    private boolean isFocusToken(TokenCardMeta t)
    {
        return focusToken != null && focusToken.equals(t);
    }

    /**
     * This method will add unknown token to the list and discover it
     * @param cAddr Contract Address
     */
    public void addUnknownTokenToCheck(ContractAddress cAddr)
    {
        for (ContractAddress check : unknownTokens)
        {
            if (check.chainId == cAddr.chainId && check.address.equalsIgnoreCase(cAddr.address))
            {
                return;
            }
        }

        if (getToken(cAddr.chainId, cAddr.address) == null)
        {
            unknownTokens.addLast(cAddr);
            startUnknownCheck();
        }
    }

    public void addUnknownTokenToCheckPriority(ContractAddress cAddr)
    {
        for (ContractAddress check : unknownTokens)
        {
            if (check.chainId == cAddr.chainId && (check.address == null || check.address.equalsIgnoreCase(cAddr.address)))
            {
                return;
            }
        }

        if (getToken(cAddr.chainId, cAddr.address) == null)
        {
            unknownTokens.addFirst(cAddr);
            startUnknownCheck();
        }
    }

    private void startUnknownCheck()
    {
        if (checkUnknownTokenCycle == null || checkUnknownTokenCycle.isDisposed())
        {
            checkUnknownTokenCycle = Observable.interval(1000, 500, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> checkUnknownTokens()).subscribe();
        }
    }

    private void startupPass()
    {
        if (!walletStartup) return;

        walletStartup = false;

        //one time pass over tokens with a null name
        tokenRepository.fetchAllTokensWithBlankName(currentAddress, networkFilter)
                .map(contractAddrs -> {
                    Collections.addAll(unknownTokens, contractAddrs);
                    startUnknownCheck();
                    return 1;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe()
                .isDisposed();
    }

    public List<Long> getNetworkFilters()
    {
        return networkFilter;
    }

    public String getNetworkName(long chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info != null) return info.getShortName();
        else return "";
    }

    public String getNetworkSymbol(long chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info == null) { info = ethereumNetworkRepository.getNetworkByChain(MAINNET_ID); }
        return info.symbol;
    }

    public void addTokenImageUrl(long networkId, String address, String imageUrl)
    {
        tokenRepository.addImageUrl(networkId, address, imageUrl);
    }

    public Single<TokenInfo> update(String address, long chainId) {
        return tokenRepository.update(address, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void checkIssueTokens()
    {
        if (openseaService == null) return;
        tokenRepository.fetchTokensThatMayNeedUpdating(currentAddress, networkFilter)
                .map(tokens -> {
                    for (Token t : tokens)
                    {
                        storeToken(t);
                    }
                    return tokens;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .isDisposed();
    }

    private void addUnresolvedContracts(List<ContractLocator> contractCandidates)
    {
        if (openseaService == null) return; //no need for this if syncing
        if (contractCandidates != null && contractCandidates.size() > 0)
        {
            for (ContractLocator cl : contractCandidates)
            {
                if (getToken(cl.chainId, cl.address) == null)
                {
                    addUnknownTokenToCheck(new ContractAddress(cl.chainId, cl.address));
                }
            }
        }
    }

    private void checkTokensBalance()
    {
        final Token t = getNextInBalanceUpdateQueue();

        if (t != null)
        {
            Timber.tag(TAG).d("Updating: " + t.tokenInfo.chainId + (t.isEthereum() ? " (Base Chain) ":"") + " : " + t.getAddress() + " : " + t.getFullName());
            balanceCheckDisposable = tokenRepository.updateTokenBalance(currentAddress, t)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(newBalance -> onBalanceChange(newBalance, t), this::onError);
        }

        checkPendingChains();
    }

    public Single<BigDecimal> getChainBalance(String walletAddress, long chainId)
    {
        return tokenRepository.fetchChainBalance(walletAddress, chainId);
    }

    private void onBalanceChange(BigDecimal newBalance, Token t)
    {
        boolean balanceChange = !newBalance.equals(t.balance);

        if (balanceChange && BuildConfig.DEBUG)
        {
            Timber.tag(TAG).d("Change Registered: * %s", t.getFullName());
        }

        //update check time
        pendingTokenMap.put(databaseKey(t), System.currentTimeMillis());

        //Switch this token chain on
        if (t.isEthereum() && newBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            checkChainVisibility(t);
            if (syncCount == 0 && completionCallback != null) { completionCallback.syncComplete(this, -1); }
        }

        if (t.isEthereum())
        {
            checkERC20(t.tokenInfo.chainId);
        }

        checkOpenSea(t.tokenInfo.chainId);
    }

    private void checkChainVisibility(Token t)
    {
        //Switch this token chain on
        if (!networkFilter.contains(t.tokenInfo.chainId) && EthereumNetworkRepository.hasRealValue(t.tokenInfo.chainId) == this.mainNetActive)
        {
            Timber.tag(TAG).d("Detected balance");
            //activate this filter
            networkFilter.add(t.tokenInfo.chainId);
            //now update the default filters
            ethereumNetworkRepository.setFilterNetworkList(networkFilter.toArray(new Long[0]));
        }
    }

    private void checkPendingChains()
    {
        long currentTime = System.currentTimeMillis();
        for (Long chainId : pendingChainMap.keySet())
        {
            if (currentTime > pendingChainMap.get(chainId))
            {
                pendingChainMap.remove(chainId);
            }
        }
    }

    private void onError(Throwable throwable)
    {
        Timber.e(throwable);
    }

    private void checkOpenSea(long chainId)
    {
        if ((openSeaQueryDisposable != null && !openSeaQueryDisposable.isDisposed())
            || openseaService == null || !EthereumNetworkBase.hasOpenseaAPI(chainId)
            || !openseaService.canCheckChain(chainId)) return;

        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);

        if (info.chainId == transferCheckChain) return; //currently checking this chainId in TransactionsNetworkClient

        final Wallet wallet = new Wallet(currentAddress);
        
        Timber.tag(TAG).d("Fetch from opensea : " + currentAddress + " : " + info.getShortName());

        openSeaCheckId = info.chainId;

        openSeaQueryDisposable = openseaService.getTokens(currentAddress, info.chainId, info.getShortName(), this)
                .flatMap(tokens -> tokenRepository.checkInterface(tokens, wallet)) //check the token interface
                .map(tokens -> tokenRepository.initNFTAssets(wallet, tokens))
                .flatMap(tokens -> tokenRepository.storeTokens(wallet, tokens)) //store fetched tokens
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(t -> checkedNetwork(info),
                        this::chuckError);
    }

    public boolean openSeaUpdateInProgress(long chainId)
    {
        return openSeaQueryDisposable != null && !openSeaQueryDisposable.isDisposed() && openSeaCheckId == chainId;
    }

    private void checkedNetwork(NetworkInfo info)
    {
        openSeaQueryDisposable = null;
        openSeaCheckId = 0;
        Timber.tag(TAG).d("Checked " + info.name + " Opensea");
    }

    private void chuckError(@NotNull Throwable e)
    {
        openSeaCheckId = 0;
        openSeaQueryDisposable = null;
        Timber.e(e);
    }

    private void checkERC20(long chainId)
    {
        if (erc20CheckDisposable == null || erc20CheckDisposable.isDisposed())
        {
            erc20CheckDisposable = tickerService.syncERC20Tickers(chainId, getAllERC20(chainId))
                    .subscribeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::finishCheckChain, this::onERC20Error);
        }
    }

    private List<TokenCardMeta> getAllERC20(long chainId)
    {
        TokenCardMeta[] tokenList = tokenRepository.fetchTokenMetasForUpdate(new Wallet(currentAddress), Collections.singletonList(chainId));
        List<TokenCardMeta> allERC20 = new ArrayList<>();
        for (TokenCardMeta tcm : tokenList)
        {
            if (tcm.type == ContractType.ERC20 && tcm.isEnabled) //filter out enabled, visible tokens
            {
                allERC20.add(tcm);
            }
        }
        return allERC20;
    }

    private void finishCheckChain(int updated)
    {
        erc20CheckDisposable = null;
    }

    private void onERC20Error(Throwable throwable)
    {
        erc20CheckDisposable = null;
        Timber.e(throwable);
    }

    public void updateTickers()
    {
        tickerService.updateTickers();
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokenRepository.getRealmInstance(wallet);
    }

    public Realm getWalletRealmInstance()
    {
        if (currentAddress != null)
        {
            return tokenRepository.getRealmInstance(new Wallet(currentAddress));
        }
        else
        {
            return null;
        }
    }

    /**
     * Called when we create a transaction
     * @param chainId
     */
    public void markChainPending(long chainId)
    {
        pendingChainMap.put(chainId, System.currentTimeMillis() + PENDING_TIME_LIMIT);
    }

    public Single<Pair<Double, Double>> getFiatValuePair()
    {
        return tokenRepository.getTotalValue(currentAddress, EthereumNetworkBase.getAllMainNetworks());
    }

    public Single<List<String>> getTickerUpdateList()
    {
        return tokenRepository.getTickerUpdateList(networkFilter);
    }

    public double convertToUSD(double localFiatValue)
    {
        return localFiatValue / tickerService.getCurrentConversionRate();
    }

    public Pair<Double, Double> getFiatValuePair(long chainId, String address)
    {
        Token token = getToken(chainId, address);
        TokenTicker tt = token != null ? getTokenTicker(token) : null;
        if (tt == null) return new Pair<>(0.0, 0.0);

        return new Pair<>(Double.parseDouble(tt.price), Double.parseDouble(tt.percentChange24h));
    }

    public double getTokenFiatValue(long chainId, String address)
    {
        Token token = getToken(chainId, address);
        TokenTicker tt = token != null ? getTokenTicker(token) : null;
        if (tt == null) return 0.0;
        BigDecimal correctedBalance = token.getCorrectedBalance(18);
        BigDecimal fiatValue = correctedBalance.multiply(new BigDecimal(tt.price)).setScale(18, RoundingMode.DOWN);
        return fiatValue.doubleValue();
    }

    ///////////////////////////////////////////
    // Update Heuristics - timings and weightings for token updates
    // Fine tune how and when tokens are updated here

    /**
     * Token update heuristic - calculates which token should be updated next
     * @return Token that needs updating
     */

    //TODO: Integrate the transfer check update time into the priority calculation
    //TODO: If we have done a transfer check recently then we don't need to check balance here
    public Token getNextInBalanceUpdateQueue()
    {
        //pull all tokens from this wallet out of DB
        TokenCardMeta[] tokenList = buildUpdateMap();

        //calculate update based on last update time & importance
        float highestWeighting = 0;
        long currentTime = System.currentTimeMillis();
        Token storeToken = pendingBaseCheck();
        if (storeToken == null) { storeToken = tokenStoreList.poll(); }
        if (storeToken != null) { return storeToken; }

        TokenCardMeta highestToken = null;

        //this list will be in order of update.
        for (TokenCardMeta check : tokenList)
        {
            long lastCheckDiff = currentTime - check.lastUpdate;
            long lastUpdateDiff = check.lastTxUpdate > 0 ? currentTime - check.lastTxUpdate : 0;

            float weighting = check.calculateBalanceUpdateWeight();

            if ((!check.isEnabled || check.isNFT()) && !isSynced()) continue; //don't start looking at NFT balances until we sync the chain/ERC20 tokens
            if (!isSynced() && check.lastUpdate > syncStart) continue; //don't start updating already updated tokens until all ERC20 are checked
            if (!appHasFocus && (!check.isEthereum() && !isFocusToken(check))) continue; //only check chains when wallet out of focus

            //simply multiply the weighting by the last diff.
            float updateFactor = weighting * (float) lastCheckDiff * (check.isEnabled ? 1 : 0.25f);
            long cutoffCheck = 30*DateUtils.SECOND_IN_MILLIS / (check.isEnabled ? 1 : 10); //normal minimum update frequency for token 30 seconds, 5 minutes for hidden token

            if (!check.isEthereum() && lastUpdateDiff > DateUtils.DAY_IN_MILLIS)
            {
                cutoffCheck = 120*DateUtils.SECOND_IN_MILLIS;
                updateFactor = 0.5f * updateFactor;
            }

            if (isFocusToken(check))
            {
                updateFactor = 3.0f * (float) lastCheckDiff;
                cutoffCheck = 15*DateUtils.SECOND_IN_MILLIS; //focus token can be checked every 15 seconds - focus token when erc20 or chain clicked on in wallet
            }
            else if (check.isEthereum() && pendingChainMap.containsKey(check.getChain())) //higher priority for checking balance of pending chain
            {
                cutoffCheck = 15*DateUtils.SECOND_IN_MILLIS;
                updateFactor = 4.0f * (float) lastCheckDiff; //chain has a recent transaction
            }
            else if (check.isEthereum())
            {
                cutoffCheck = 20*DateUtils.SECOND_IN_MILLIS; //update check limit for base chains is 20 seconds
            }
            else if (focusToken != null)
            {
                updateFactor = 0.1f * (float) lastCheckDiff;
                cutoffCheck = 60*DateUtils.SECOND_IN_MILLIS; //when looking at token in detail view (ERC20TokenDetail) update other tokens at 1 minute cycle
            }

            if (updateFactor > highestWeighting && (lastCheckDiff > (float)cutoffCheck))
            {
                highestWeighting = updateFactor;
                highestToken = check;
            }
        }

        if (highestToken != null)
        {
            pendingTokenMap.put(databaseKey(highestToken.getChain(), highestToken.getAddress()), System.currentTimeMillis());
            return getToken(highestToken.getChain(), highestToken.getAddress());
        }
        else
        {
            return null;
        }
    }

    private Token pendingBaseCheck()
    {
        Long chainId = baseTokenCheck.poll();
        if (chainId != null)
        {
            Timber.tag(TAG).d("Base Token Check: %s", ethereumNetworkRepository.getNetworkByChain(chainId).name);
            //return new TokenCardMeta(getToken(chainId, currentAddress));
            return createCurrencyToken(ethereumNetworkRepository.getNetworkByChain(chainId), new Wallet(currentAddress));
        }
        else
        {
            if (syncCount == 0)
            {
                syncCount = 1;
            }
            return null;
        }
    }

    /**
     * Get network filter settings and initialise checks for token balances
     */
    private void setupFilters()
    {
        baseTokenCheck.clear();
        mainNetActive = ethereumNetworkRepository.isMainNetSelected();
        if (!ethereumNetworkRepository.hasSetNetworkFilters()) //add all networks to a check list to check balances at wallet startup and refresh
        {
            //first blank all existing filters for zero balance tokens, as user hasn't specified which chains they want to see
            blankFiltersForZeroBalance(mainNetActive);

            NetworkInfo[] networks = ethereumNetworkRepository.getAllActiveNetworks();
            for (NetworkInfo info : networks) { baseTokenCheck.add(info.chainId); }
        }
    }

    /**
     * set up visibility only for tokens with balance, if all zero then add the default networks
     * @param mainNetActive
     */
    private void blankFiltersForZeroBalance(boolean mainNetActive)
    {
        networkFilter.clear();
        NetworkInfo[] networks = ethereumNetworkRepository.getAllActiveNetworks();

        if (!ethereumNetworkRepository.hasSetNetworkFilters())
        {
            for (NetworkInfo network : networks)
            {
                Token t = getToken(network.chainId, currentAddress);
                if (t != null && t.balance.compareTo(BigDecimal.ZERO) > 0)
                {
                    networkFilter.add(network.chainId);
                }
            }
        }

        for (Long lockedChain : CustomViewSettings.getLockedChains())
        {
            if (!networkFilter.contains(lockedChain)) networkFilter.add(lockedChain);
        }

        if (networkFilter.size() == 0) networkFilter.add(ethereumNetworkRepository.getDefaultNetwork(mainNetActive));

        //set network filter prefs
        ethereumNetworkRepository.setFilterNetworkList(networkFilter.toArray(new Long[0]));
    }

    /**
     * Provides an immediate find token and add to Realm call
     * - As opposed to using storeToken which is a lower priority 'add token to standard update queue'
     *
     * @param info TokenInfo
     * @param walletAddress Current Wallet Address
     * @return RX-Single token return
     */
    public Single<Token> addToken(final TokenInfo info, final String walletAddress)
    {
        return tokenRepository.determineCommonType(info)
                .map(contractType -> tokenFactory.createToken(info, contractType, ethereumNetworkRepository.getNetworkByChain(info.chainId).getShortName()))
                .flatMap(token -> tokenRepository.updateTokenBalance(walletAddress, token).map(newBalance -> {
                    token.balance = newBalance;
                    return token;
                }));
    }

    public void addTokens(List<Token> tokenList)
    {
        for (Token t : tokenList)
        {
            if (t != null) tokenStoreList.addFirst(t);
        }
    }

    //Add in any tokens required to be shown - mainly used by forks for always showing a specific token
    //Note that we can't go via the usual tokenStoreList method as we need to mark this token as enabled and locked visible
    private void addLockedTokens()
    {
        mainNetActive = ethereumNetworkRepository.isMainNetSelected();
        final String wallet = currentAddress;
        //ensure locked tokens are displaying
        Observable.fromIterable(CustomViewSettings.getLockedTokens())
                .forEach(info -> addToken(info, wallet)
                        .flatMapCompletable(token -> enableToken(wallet, token))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe())
                        .isDisposed();
    }

    private Completable enableToken(String walletAddr, Token token)
    {
        return Completable.fromAction(() -> {
            final Wallet wallet = new Wallet(walletAddr);
            tokenRepository.setEnable(wallet, token, true);
            tokenRepository.setVisibilityChanged(wallet, token);
        });
    }

    public Completable lockTokenVisibility(Token token)
    {
        return enableToken(currentAddress, token);
    }

    /**
     * Timings for when there can be a check for new transactions
     * @param t
     * @return
     */
    private long getTokenTimeInterval(Token t)
    {
        long nextTimeCheck;

        if (t.isEthereum())
        {
            nextTimeCheck = 30*DateUtils.SECOND_IN_MILLIS; //allow base chains to be checked about every 30 seconds
        }
        else
        {
            nextTimeCheck = t.getTransactionCheckInterval();
        }

        return nextTimeCheck;
    }

    public Realm getTickerRealmInstance()
    {
        return tokenRepository.getTickerRealmInstance();
    }

    public boolean shouldDisplayPopularToken(TokenCardMeta tcm)
    {
        //Display popular token if
        // - explicitly enabled
        // - user has not altered the visibility and token has positive balance (user may not be aware of visibility controls).
        if (ethereumNetworkRepository.getIsPopularToken(tcm.getChain(), tcm.getAddress()))
        {
            Token token = getToken(tcm.getChain(), tcm.getAddress());
            return (token == null)
                    || tokenRepository.isEnabled(token)
                    || (!tokenRepository.hasVisibilityBeenChanged(token) && token.hasPositiveBalance());
        }
        else
        {
            return true;
        }
    }

    public void walletInFocus()
    {
        appHasFocus = true;

        //running or not?
    }

    public boolean isMainNetActive()
    {
        return mainNetActive;
    }

    public void walletOutOfFocus()
    {
        appHasFocus = false;
    }

    /**
     * Notify that the new gas setting widget was actually used :)
     *
     * @param gasSpeed
     */
    public void track(String gasSpeed)
    {
        if (analyticsService != null)
        {
            AnalyticsProperties analyticsProperties = new AnalyticsProperties();
            analyticsProperties.setData(gasSpeed);

            analyticsService.track(C.AN_USE_GAS, analyticsProperties);
        }
    }

    public Token getTokenOrBase(long chainId, String address)
    {
        Token token = getToken(chainId, address);
        if (token == null)
        {
            token = getToken(chainId, currentAddress); // use base currency
        }

        if (token == null)
        {
            //create base token if required
            token = ethereumNetworkRepository.getBlankOverrideToken(ethereumNetworkRepository.getNetworkByChain(chainId));
        }

        return token;
    }

    public void updateAssets(Token token, List<BigInteger> additions, List<BigInteger> removals)
    {
        tokenRepository.updateAssets(currentAddress, token, additions, removals);
    }

    public void storeAsset(Token token, BigInteger tokenId, NFTAsset asset)
    {
        tokenRepository.storeAsset(currentAddress, token, tokenId, asset);
    }

    public boolean isChainToken(long chainId, String tokenAddress)
    {
        return ethereumNetworkRepository.isChainContract(chainId, tokenAddress);
    }

    public boolean hasChainToken(long chainId)
    {
        return EthereumNetworkRepository.getChainOverrideAddress(chainId).length() > 0;
    }

    public Token getServiceToken(long chainId)
    {
        if (hasChainToken(chainId))
        {
            return getToken(chainId, EthereumNetworkRepository.getChainOverrideAddress(chainId));
        }
        else
        {
            return getToken(chainId, currentAddress);
        }
    }

    public String getFallbackUrlForToken(Token token)
    {
        String tURL = tokenRepository.getTokenImageUrl(token.tokenInfo.chainId, token.getAddress());
        if (TextUtils.isEmpty(tURL))
        {
            tURL = Utils.getTWTokenImageUrl(token.tokenInfo.chainId, token.getAddress());
        }

        return tURL;
    }

    public void checkingChain(long chainId)
    {
        transferCheckChain = chainId;
    }

    public void addBalanceCheck(Token token)
    {
        for (Token t : tokenStoreList)
        {
            if (t.equals(token)) return;
        }

        tokenStoreList.add(token);
    }

    private Token createCurrencyToken(NetworkInfo network, Wallet wallet)
    {
        TokenInfo tokenInfo = new TokenInfo(wallet.address, network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = BigDecimal.ZERO;
        Token eth = new Token(tokenInfo, balance, 0, network.getShortName(), ContractType.ETHEREUM); //create with zero time index to ensure it's updated immediately
        eth.setTokenWallet(wallet.address);
        eth.setIsEthereum();
        eth.pendingBalance = balance;
        return eth;
    }

    public boolean isSynced()
    {
        return (syncTimer == 0);
    }

    public boolean startWalletSync(ServiceSyncCallback cb)
    {
        if (ethereumNetworkRepository.isMainNetSelected())
        {
            setCompletionCallback(cb, 0);
            return true;
        }
        else
        {
            completionCallback = cb;
            return false;
        }
    }

    public void setCompletionCallback(ServiceSyncCallback cb, int sync)
    {
        syncCount = sync;
        completionCallback = cb;
        syncTimer = System.currentTimeMillis();

        //Setup
        baseTokenCheck.clear();
        mainNetActive = true;
        networkFilter.clear();

        NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();

        for (NetworkInfo info : networks)
        {
            if (info.hasRealValue())
            {
                networkFilter.add(info.chainId);
                baseTokenCheck.add(info.chainId);
            }
        }
    }

    private void deleteTickers()
    {
        if (BuildConfig.DEBUG && !done) //Ensure release build never deletes all the tickers
        {
            done = true;
            Single.fromCallable(() -> {
                tickerService.deleteTickers();
                return true;
            }).subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io()).subscribe(b -> {
            }).isDisposed();
        }
    }

    // TODO: This may be refactored once we have switched over to a more efficient Token database model
    // That is - common data like Name, Decimals, Address goes into a single Token database,
    //   wallet specific data like balance, update time etc goes into the per-wallet database
    public TokenGroup getTokenGroup(Token token)
    {
        return tokenRepository.getTokenGroup(token.tokenInfo.chainId, token.tokenInfo.address, token.getInterfaceSpec());
    }

}
