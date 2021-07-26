package com.alphawallet.app.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.token.entity.ContractAddress;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class TokensService
{
    private static final String TAG = "TOKENSSERVICE";
    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";
    public static final long PENDING_TIME_LIMIT = 3*DateUtils.MINUTE_IN_MILLIS; //cut off pending chain after 3 minutes

    private static final Map<String, Float> tokenValueMap = new ConcurrentHashMap<>(); //this is used to compute the USD value of the tokens on an address
    private static final Map<Integer, Long> pendingChainMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private final Map<String, Token> tokenStoreList = new ConcurrentHashMap<>(); //used to hold tokens that will be stored
    private String currentAddress = null;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;
    private final Context context;
    private final TickerService tickerService;
    private final OpenseaService openseaService;
    private final AnalyticsServiceType<AnalyticsProperties> analyticsService;
    private final List<Integer> networkFilter;
    private ContractLocator focusToken;
    private final ConcurrentLinkedDeque<ContractAddress> unknownTokens;
    private final ConcurrentLinkedQueue<Integer> baseTokenCheck;
    private long nextTokenCheck;
    private static boolean openSeaCheck;
    private boolean appHasFocus = true;
    private boolean mainNetActive = true;
    private static boolean walletStartup = false;

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
    private Disposable storeErc20Tokens;
    @Nullable
    private Disposable openseaCheck;

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository,
                         TokenRepositoryType tokenRepository,
                         Context context,
                         TickerService tickerService,
                         OpenseaService openseaService,
                         AnalyticsServiceType<AnalyticsProperties> analyticsService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
        this.context = context;
        this.tickerService = tickerService;
        this.openseaService = openseaService;
        this.analyticsService = analyticsService;
        networkFilter = new ArrayList<>();
        setupFilter();
        focusToken = null;
        this.unknownTokens = new ConcurrentLinkedDeque<>();
        this.baseTokenCheck = new ConcurrentLinkedQueue<>();
        setCurrentAddress(ethereumNetworkRepository.getCurrentWalletAddress()); //set current wallet address at service startup
        appHasFocus = true;
    }

    private void checkUnknownTokens()
    {
        if (queryUnknownTokensDisposable == null || queryUnknownTokensDisposable.isDisposed())
        {
            ContractAddress t = unknownTokens.pollFirst();
            Token cachedToken = t != null ? getToken(t.chainId, t.address) : null;

            if (t != null && (cachedToken == null || TextUtils.isEmpty(cachedToken.tokenInfo.name)))
            {
                queryUnknownTokensDisposable = tokenRepository.update(t.address, t.chainId).toObservable() //fetch tokenInfo
                        .filter(tokenInfo -> tokenInfo.name != null)
                        .map(tokenInfo -> { tokenInfo.isEnabled = false; return tokenInfo; }) //set default visibility to false
                        .flatMap(tokenInfo -> tokenRepository.determineCommonType(tokenInfo).toObservable()
                            .flatMap(contractType -> tokenRepository.addToken(new Wallet(currentAddress), tokenInfo, contractType).toObservable()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(this::finishAddToken, this::onCheckError, this::finishTokenCheck);
            }
            else if (t == null)
            {
                //stop the check
                if (checkUnknownTokenCycle != null && !checkUnknownTokenCycle.isDisposed())
                    checkUnknownTokenCycle.dispose();
            }
        }
    }

    private void onCheckError(Throwable throwable)
    {
        if (BuildConfig.DEBUG) throwable.printStackTrace();
    }

    private void finishTokenCheck()
    {
        queryUnknownTokensDisposable = null;
    }

    // Add resolved token to service and notify views of new token
    private void finishAddToken(Token token)
    {
        if (token.getInterfaceSpec() != ContractType.OTHER)
        {
            Intent intent = new Intent(ADDED_TOKEN);
            intent.putParcelableArrayListExtra(C.EXTRA_TOKENID_LIST, new ArrayList<>(Collections.singletonList(new ContractLocator(token.getAddress(), token.tokenInfo.chainId, token.getInterfaceSpec()))));
            context.sendBroadcast(intent);
            //now add to the balance update list if has balance
        }
    }

    public Token getToken(int chainId, String addr)
    {
        if (TextUtils.isEmpty(currentAddress) || TextUtils.isEmpty(addr)) return null;
        else return tokenRepository.fetchToken(chainId, currentAddress, addr.toLowerCase());
    }

    public void storeToken(Token token)
    {
        if (TextUtils.isEmpty(currentAddress) || token == null || token.getInterfaceSpec() == ContractType.OTHER) return;
        //store token to database, update balance
        //check for duplicates
        addToTokenStoreList(token);
        if (storeErc20Tokens == null)
        {
            storeErc20Tokens = Observable.interval(0, 50, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> storeNextToken()).subscribeOn(Schedulers.newThread()).subscribe();
        }
    }

    private void addToTokenStoreList(Token token)
    {
        String key = TokensRealmSource.databaseKey(token);
        Token existing = tokenStoreList.get(key);

        if (existing == null || (existing.getInterfaceSpec() != token.getInterfaceSpec() && existing.getInterfaceSpec() == ContractType.ERC20))
        {
            tokenStoreList.put(key, token);
        }
    }

    private void storeNextToken()
    {
        //process chains in order
        if (tokenStoreList.keySet().iterator().hasNext())
        {
            String key = tokenStoreList.keySet().iterator().next();
            Token t = tokenStoreList.get(key);
            Wallet wallet = new Wallet(t.getWallet());
            tokenStoreList.remove(key);
            tokenRepository.checkInterface(new Token[]{t}, wallet) //if ERC721 determine the specific contract type
                    .flatMap(tkns -> tokenRepository.storeTokens(wallet, tkns))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::storedToken, Throwable::printStackTrace)
                    .isDisposed();
        }
        else if (storeErc20Tokens != null && !storeErc20Tokens.isDisposed())
        {
            storeErc20Tokens.dispose();
            storeErc20Tokens = null;
        }
    }

    private void storedToken(Token[] tokens)
    {

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
        for (int chainId : networkFilter)
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
            openSeaCheck = true;
            addLockedTokens();
        }
    }

    public void startUpdateCycle()
    {
        if (currentAddress == null) return;
        startupPass();
        populateTokenCheck();

        if (nextTokenCheck == 0) nextTokenCheck = System.currentTimeMillis() + 2*DateUtils.SECOND_IN_MILLIS; //delay first checking of Opensea/ERC20 to allow wallet UI to startup

        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
        }

        if (balanceCheckDisposable != null && !balanceCheckDisposable.isDisposed()) balanceCheckDisposable.dispose();
        if (erc20CheckDisposable != null && !erc20CheckDisposable.isDisposed()) erc20CheckDisposable.dispose();

        addUnresolvedContracts(ethereumNetworkRepository.getAllKnownContracts(getNetworkFilters()));
        checkIssueTokens();

        eventTimer = Observable.interval(1, 500, TimeUnit.MILLISECONDS)
                .doOnNext(l -> checkTokensBalance())
                .observeOn(Schedulers.newThread()).subscribe();
    }

    public void stopUpdateCycle()
    {
        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
            eventTimer = null;
        }

        IconItem.resetCheck();
        tokenValueMap.clear();
        pendingChainMap.clear();
        tokenStoreList.clear();
        baseTokenCheck.clear();
    }

    public String getCurrentAddress() { return currentAddress; }

    public static void setInterfaceSpec(int chainId, String address, ContractType functionSpec)
    {
        SparseArray<ContractType> types = interfaceSpecMap.get(address);
        if (types == null)
        {
            types = new SparseArray<>();
            interfaceSpecMap.put(address, types);
        }
        types.put(chainId, functionSpec);
    }

    public static ContractType checkInterfaceSpec(int chainId, String address)
    {
        SparseArray<ContractType> types = interfaceSpecMap.get(address);
        ContractType type = types != null ? types.get(chainId) : null;
        if (type != null)
        {
            return type;
        }
        else
        {
            return ContractType.NOT_SET;
        }
    }

    public static void setWalletStartup() { walletStartup = true; }

    public void setupFilter()
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
    }

    public void setFocusToken(@NotNull Token token)
    {
        focusToken = new ContractLocator(token.getAddress(), token.tokenInfo.chainId);
    }

    public void clearFocusToken()
    {
        focusToken = null;
    }

    private boolean isFocusToken(Token t)
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
            checkUnknownTokenCycle = Observable.interval(0, 500, TimeUnit.MILLISECONDS)
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
                    for (ContractAddress addr : contractAddrs) { unknownTokens.add(addr); }
                    startUnknownCheck();
                    return 1;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe()
                .isDisposed();
    }

    public List<Integer> getNetworkFilters()
    {
        return networkFilter;
    }

    public String getNetworkName(int chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info != null) return info.getShortName();
        else return "";
    }

    public String getNetworkSymbol(int chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info == null) { info = ethereumNetworkRepository.getNetworkByChain(MAINNET_ID); }
        return info.symbol;
    }

    public void addTokenImageUrl(int networkId, String address, String imageUrl)
    {
        tokenRepository.addImageUrl(networkId, address, imageUrl);
    }

    public Single<TokenInfo> update(String address, int chainId) {
        return tokenRepository.update(address, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void checkIssueTokens()
    {
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
        Token t = getNextInBalanceUpdateQueue();

        if (t != null)
        {
            if (BuildConfig.DEBUG) Log.d(TAG, "Updating: " + t.tokenInfo.chainId + (t.isEthereum() ? " (Base Chain) ":"") + " : " + t.getAddress() + " : " + t.getFullName());
            balanceCheckDisposable = tokenRepository.updateTokenBalance(currentAddress, t.tokenInfo.chainId, t.getAddress(), t.getInterfaceSpec())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(newBalance -> onBalanceChange(newBalance, t), this::onError);
        }

        if (System.currentTimeMillis() > nextTokenCheck)
        {
            checkERC20();
        }
        else if (openSeaCheck)
        {
            checkOpenSea();
        }

        checkPendingChains();
    }

    public Single<BigDecimal> getChainBalance(String walletAddress, int chainId)
    {
        return tokenRepository.fetchChainBalance(walletAddress, chainId);
    }

    private void onBalanceChange(BigDecimal newBalance, Token t)
    {
        boolean balanceChange = !newBalance.equals(t.balance);
        // could still be pending transactions so let's keep checking for a short while
        if (balanceChange && BuildConfig.DEBUG) Log.d(TAG, "Change Registered: * " + t.getFullName());

        //Switch this token chain on
        if (t.isEthereum() && newBalance.compareTo(BigDecimal.ZERO) > 0)
        {
            checkChainVisibility(t);
        }
    }

    private void checkChainVisibility(Token t)
    {
        //Switch this token chain on
        if (!networkFilter.contains(t.tokenInfo.chainId) && EthereumNetworkRepository.hasRealValue(t.tokenInfo.chainId) == this.mainNetActive)
        {
            if (BuildConfig.DEBUG) Log.d(TAG, "Detected balance");
            //activate this filter
            networkFilter.add(t.tokenInfo.chainId);
            //now update the default filters
            ethereumNetworkRepository.setFilterNetworkList(networkFilter.toArray(new Integer[0]));
        }
    }

    private void checkPendingChains()
    {
        long currentTime = System.currentTimeMillis();
        for (Integer chainId : pendingChainMap.keySet())
        {
            if (currentTime > pendingChainMap.get(chainId))
            {
                pendingChainMap.remove(chainId);
            }
        }
    }

    private void onError(Throwable throwable)
    {
        if (BuildConfig.DEBUG) throwable.printStackTrace();
    }

    private void checkOpenSea()
    {
        if (networkFilter.contains(MAINNET_ID) && (openseaCheck == null || openseaCheck.isDisposed()))
        {
            NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(MAINNET_ID);
            final Wallet wallet = new Wallet(currentAddress);

            openSeaCheck = false;

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Fetch from opensea : " + currentAddress + " : " + info.getShortName());

            openseaCheck = openseaService.getTokens(currentAddress, info.chainId, info.getShortName(), this)
                    .flatMap(tokens -> tokenRepository.checkInterface(tokens, wallet)) //check the token interface
                    .flatMap(tokens -> tokenRepository.storeTokens(wallet, tokens)) //store fetched tokens
                    .map(tokens -> tokenRepository.initERC721Assets(wallet, tokens))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(t -> checkedNetwork(),
                            this::chuckError);
        }
    }

    private void checkedNetwork()
    {
        openseaCheck = null;
        if (BuildConfig.DEBUG) Log.d(TAG, "Checked Mainnet Opensea");
    }

    private void chuckError(@NotNull Throwable e)
    {
        openseaCheck = null;
        e.printStackTrace();
    }

    private void checkERC20()
    {
        if (erc20CheckDisposable == null || erc20CheckDisposable.isDisposed())
        {
            nextTokenCheck = System.currentTimeMillis() + 30 * DateUtils.SECOND_IN_MILLIS;
            //get mainnet ERC20 token tickers
            erc20CheckDisposable = tickerService.getERC20Tickers(getAllERC20(MAINNET_ID))
                    .subscribeOn(Schedulers.io())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::finishCheckChain, this::onERC20Error);
        }
    }

    private List<TokenCardMeta> getAllERC20(int chainId)
    {
        TokenCardMeta[] tokenList = tokenRepository.fetchTokenMetasForUpdate(new Wallet(currentAddress), Collections.singletonList(chainId));
        List<TokenCardMeta> allERC20 = new ArrayList<>();
        for (TokenCardMeta tcm : tokenList)
        {
            if (tcm.type == ContractType.ERC20)
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
        if (BuildConfig.DEBUG) throwable.printStackTrace();
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
    public void markChainPending(int chainId)
    {
        pendingChainMap.put(chainId, System.currentTimeMillis() + PENDING_TIME_LIMIT);
    }

    public void addTokenValue(int chainId, String tokenAddress, float value)
    {
        if (EthereumNetworkRepository.hasRealValue(chainId))
        {
            if (tokenAddress.equalsIgnoreCase(currentAddress)) tokenAddress = String.valueOf(chainId);
            tokenValueMap.put(tokenAddress, value);
        }
    }

    public double getUSDValue()
    {
        double totalVal = 0.0f;
        for (Float val : tokenValueMap.values())
        {
            totalVal += val;
        }

        return totalVal*tickerService.getCurrentConversionRate();
    }

    public void walletHidden()
    {
        //stop updates (note that for notifications we'll use a background service)
        stopUpdateCycle();
    }

    public void walletShowing()
    {
        //restart the event cycle
        startUpdateCycle();
    }

    ///////////////////////////////////////////
    // Update Heuristics - timings and weightings for token updates
    // Fine tune how and when tokens are updated here

    /**
     * Token update heuristic - calculates which token should be updated next
     * @return
     */
    public Token getNextInBalanceUpdateQueue()
    {
        //pull all tokens from this wallet out of DB
        TokenCardMeta[] tokenList = tokenRepository.fetchTokenMetasForUpdate(new Wallet(currentAddress), networkFilter);

        //calculate update based on last update time & importance
        float highestWeighting = 0;
        long currentTime = System.currentTimeMillis();
        Token highestToken = pendingBaseCheck();
        if (highestToken != null) return highestToken; //initial wallet refresh base token check

        //this list will be in order of update.
        for (TokenCardMeta check : tokenList)
        {
            Token token = getToken(check.getChain(), check.getAddress());
            if (token == null) continue;
            long lastUpdateDiff = currentTime - check.lastUpdate;
            float weighting = check.calculateBalanceUpdateWeight();

            if (!appHasFocus && (!token.isEthereum() && !isFocusToken(token))) continue; //only check chains when wallet out of focus

            //simply multiply the weighting by the last diff.
            float updateFactor = weighting * (float) lastUpdateDiff;
            long cutoffCheck = 30*DateUtils.SECOND_IN_MILLIS; //normal minimum update frequency for token 30 seconds

            if (isFocusToken(token))
            {
                updateFactor = 3.0f * (float) lastUpdateDiff;
                cutoffCheck = 15*DateUtils.SECOND_IN_MILLIS; //focus token can be checked every 15 seconds - focus token when erc20 or chain clicked on in wallet
            }
            else if (check.isEthereum() && pendingChainMap.containsKey(token.tokenInfo.chainId)) //higher priority for checking balance of pending chain
            {
                cutoffCheck = 15*DateUtils.SECOND_IN_MILLIS;
                updateFactor = 4.0f * (float) lastUpdateDiff; //chain has a recent transaction
            }
            else if (check.isEthereum())
            {
                cutoffCheck = 20*DateUtils.SECOND_IN_MILLIS; //update check limit for base chains is 20 seconds
            }
            else if (focusToken != null)
            {
                updateFactor = 0.1f * (float) lastUpdateDiff;
                cutoffCheck = 60*DateUtils.SECOND_IN_MILLIS; //when looking at token in detail view (ERC20TokenDetail) update other tokens at 1 minute cycle
            }

            if (updateFactor > highestWeighting && (lastUpdateDiff > (float)cutoffCheck))
            {
                highestWeighting = updateFactor;
                highestToken = token;
            }
        }

        return highestToken;
    }

    private Token pendingBaseCheck()
    {
        Integer chainId = baseTokenCheck.poll();
        if (chainId != null)
        {
            if (BuildConfig.DEBUG) Log.d(TAG, "Base Token Check: " + ethereumNetworkRepository.getNetworkByChain(chainId).name);
            return getToken(chainId, currentAddress);
        }
        else
        {
            return null;
        }
    }

    private void populateTokenCheck()
    {
        mainNetActive = ethereumNetworkRepository.isMainNetSelected();
        baseTokenCheck.clear();
        tokenRepository.createBaseNetworkTokens(currentAddress);

        if (!ethereumNetworkRepository.hasSetNetworkFilters()) //add all networks to a check list to check balances at wallet startup and refresh
        {
            NetworkInfo[] networks = ethereumNetworkRepository.getAllActiveNetworks();
            for (NetworkInfo info : networks) { baseTokenCheck.add(info.chainId); }
        }
    }

    /**
     * Determine if the token or chain requires a transaction fetch
     * @param pendingTxChains
     * @return
     */
    public Token getRequiresTransactionUpdate(List<Integer> pendingTxChains)
    {
        //pull all tokens from this wallet out of DB
        TokenCardMeta[] tokenList = tokenRepository.fetchTokenMetasForUpdate(new Wallet(currentAddress), networkFilter);

        //calculate update based on last update time & importance
        long currentTime = System.currentTimeMillis();
        Token highestToken = null;
        long highestDiff = 0;

        for (TokenCardMeta check : tokenList)
        {
            Token token = getToken(check.getChain(), check.getAddress());
            if (token == null) continue;
            if (!token.needsTransactionCheck()) continue;
            long timeIntervalCheck = getTokenTimeInterval(token, pendingTxChains);
            if (timeIntervalCheck == 0) continue;

            if (focusToken != null && token.tokenInfo.chainId == focusToken.chainId)
            {
                timeIntervalCheck = 10*DateUtils.SECOND_IN_MILLIS;
            }

            if (currentTime >= (token.lastTxCheck + timeIntervalCheck))
            {
                long diff = currentTime - (token.lastTxCheck + timeIntervalCheck);
                if (diff > highestDiff)
                {
                    highestDiff = diff;
                    highestToken = token;
                }
            }
        }

        return highestToken;
    }

    private void addLockedTokens()
    {
        mainNetActive = ethereumNetworkRepository.isMainNetSelected();
        //ensure locked tokens are displaying
        Observable.fromIterable(CustomViewSettings.getLockedTokens())
                .forEach(info -> tokenRepository.determineCommonType(info)
                        .flatMap(contractType -> tokenRepository.addToken(new Wallet(currentAddress), info, contractType))
                        .flatMapCompletable(this::lockTokenVisibility)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe()).isDisposed();
    }

    private Completable lockTokenVisibility(Token token)
    {
        final Wallet wallet = new Wallet(currentAddress);
        return tokenRepository.setEnable(wallet, token, true)
                .andThen(tokenRepository.setVisibilityChanged(wallet, token))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Timings for when there can be a check for new transactions
     * @param t
     * @param pending
     * @return
     */
    private long getTokenTimeInterval(Token t, Collection<Integer> pending)
    {
        long nextTimeCheck;

        if (t.isEthereum() && pending != null && pending.contains(t.tokenInfo.chainId)) //check chain every 10 seconds while transaction is pending
        {
            nextTimeCheck = 10*DateUtils.SECOND_IN_MILLIS;
        }
        else if (t.isEthereum())
        {
            nextTimeCheck = 30*DateUtils.SECOND_IN_MILLIS; //allow base chains to be checked about every 30 seconds when not pending
        }
        else if (t != null)
        {
            nextTimeCheck = t.getTransactionCheckInterval();
        }
        else
        {
            nextTimeCheck = 0;
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

    public void appInFocus()
    {
        appHasFocus = true;
    }

    public void appOutOfFocus()
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
        AnalyticsProperties analyticsProperties = new AnalyticsProperties();
        analyticsProperties.setData(gasSpeed);

        analyticsService.track(C.AN_USE_GAS, analyticsProperties);
    }

    public Token getTokenOrBase(int chainId, String address)
    {
        Token token = getToken(chainId, address);
        if (token == null)
        {
            token = getToken(chainId, currentAddress); // use base currency
        }

        return token;
    }

    public void updateAssets(Token token, List<BigInteger> additions, List<BigInteger> removals)
    {
        tokenRepository.updateAssets(currentAddress, token, additions, removals);
    }

    public void storeAsset(Token token, Asset asset)
    {
        tokenRepository.storeAsset(currentAddress, token, asset);
    }

    public void stopOpenseaCheck()
    {
        if (openseaCheck != null && !openseaCheck.isDisposed()) openseaCheck.dispose();
    }

    public boolean isChainToken(int chainId, String tokenAddress)
    {
        return ethereumNetworkRepository.isChainContract(chainId, tokenAddress);
    }

    public boolean hasChainToken(int chainId)
    {
        return EthereumNetworkRepository.getChainOverrideAddress(chainId).length() > 0;
    }

    public Token getServiceToken(int chainId)
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
}
