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
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.token.entity.ContractAddress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.app.repository.EthereumNetworkBase.RINKEBY_ID;

public class TokensService
{
    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";
    private static final long OPENSEA_CHECK_INTERVAL = 30 * DateUtils.SECOND_IN_MILLIS;
    private static final long OPENSEA_RINKEBY_CHECK = 4; //1 in [OPENSEA_RINKEBY_CHECK] opensea calls will to Rinkeby opensea
    public static final long PENDING_TIME_LIMIT = 3*DateUtils.MINUTE_IN_MILLIS; //cut off pending chain after 3 minutes

    private static final Map<String, Float> tokenValueMap = new ConcurrentHashMap<>(); //this is used to compute the USD value of the tokens on an address
    private static final Map<Integer, Long> pendingChainMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Token> tokenStoreList = new ConcurrentLinkedQueue<>();
    private String currentAddress = null;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;
    private final Context context;
    private final TickerService tickerService;
    private final OpenseaService openseaService;
    private final AnalyticsServiceType analyticsService;
    private final List<Integer> networkFilter;
    private ContractLocator focusToken;
    private final ConcurrentLinkedDeque<ContractAddress> unknownTokens;
    private long nextOpenSeaCheck;
    private int openSeaCount;
    private boolean appHasFocus = true;

    @Nullable
    private Disposable tokenCheckDisposable;
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

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository,
                         TokenRepositoryType tokenRepository,
                         PreferenceRepositoryType preferenceRepository,
                         Context context,
                         TickerService tickerService,
                         OpenseaService openseaService,
                         AnalyticsServiceType analyticsService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
        this.context = context;
        this.tickerService = tickerService;
        this.openseaService = openseaService;
        this.analyticsService = analyticsService;
        networkFilter = new ArrayList<>();
        setupFilter();
        focusToken = null;
        setCurrentAddress(preferenceRepository.getCurrentWalletAddress()); //set current wallet address at service startup
        this.unknownTokens = new ConcurrentLinkedDeque<>();
        appHasFocus = true;
    }

    private void checkUnknownTokens()
    {
        if (queryUnknownTokensDisposable == null || queryUnknownTokensDisposable.isDisposed())
        {
            ContractAddress t = unknownTokens.pollFirst();

            if (t != null && getToken(t.chainId, t.address) == null)
            {
                queryUnknownTokensDisposable = tokenRepository.update(t.address, t.chainId).toObservable() //fetch tokenInfo
                        .filter(tokenInfo -> tokenInfo.name != null)
                        .map(tokenInfo -> { tokenInfo.isEnabled = false; return tokenInfo; }) //set default visibility to false
                        .flatMap(tokenInfo -> tokenRepository.determineCommonType(tokenInfo).toObservable()
                            .flatMap(contractType -> tokenRepository.addToken(new Wallet(currentAddress), tokenInfo, contractType).toObservable()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(this::finishAddToken, Throwable::printStackTrace, this::finishTokenCheck);
            }
            else if (t == null)
            {
                //stop the check
                if (checkUnknownTokenCycle != null && !checkUnknownTokenCycle.isDisposed())
                    checkUnknownTokenCycle.dispose();
            }
        }
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
        tokenStoreList.add(token);
        if (storeErc20Tokens == null)
        {
            storeErc20Tokens = Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> storeNextToken()).subscribe();
        }
    }

    private void storeNextToken()
    {
        Token t = tokenStoreList.poll();
        if (t != null)
        {
            Wallet wallet = new Wallet(t.getWallet());
            tokenRepository.addToken(wallet, t.tokenInfo, t.getInterfaceSpec())
                    .flatMap(token -> tokenRepository.checkInterface(new Token[]{token}, wallet)) //if ERC721 determine the specific contract type
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
        if (tokens.length > 0)
        {
            System.out.println("Stored Token: " + tokens[0].getFullName());
        }
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
            tokenValueMap.clear();
            pendingChainMap.clear();
            stopUpdateCycle();
        }
    }

    private void stopUpdateCycle()
    {
        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
            eventTimer = null;
        }
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

    public void setupFilter()
    {
        networkFilter.clear();
        networkFilter.addAll(ethereumNetworkRepository.getFilterNetworkList());
    }

    public void setFocusToken(Token token)
    {
        focusToken = new ContractLocator(token.getAddress(), token.tokenInfo.chainId);
    }

    public void clearFocusToken()
    {
        focusToken = null;
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
            if (check.chainId == cAddr.chainId && check.address.equalsIgnoreCase(cAddr.address))
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

    public void addTokenImageUrl(int networkId, String address, String imageUrl)
    {
        tokenRepository.addImageUrl(networkId, address, imageUrl);
    }

    public Single<TokenInfo> update(String address, int chainId) {
        return tokenRepository.update(address, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void startBalanceUpdate()
    {
        if (currentAddress == null) return;

        nextOpenSeaCheck = System.currentTimeMillis() + 2*DateUtils.SECOND_IN_MILLIS; //delay first checking of Opensea/ERC20 to allow wallet UI to startup
        openSeaCount = 2;

        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
        }

        if (balanceCheckDisposable != null && !balanceCheckDisposable.isDisposed()) balanceCheckDisposable.dispose();
        if (tokenCheckDisposable != null && !tokenCheckDisposable.isDisposed()) tokenCheckDisposable.dispose();
        if (erc20CheckDisposable != null && !erc20CheckDisposable.isDisposed()) erc20CheckDisposable.dispose();

        addUnresolvedContracts(ethereumNetworkRepository.getAllKnownContracts(getNetworkFilters()));

        eventTimer = Observable.interval(1, 500, TimeUnit.MILLISECONDS)
                    .doOnNext(l -> checkTokensBalance()).subscribe();
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
            if (BuildConfig.DEBUG) Log.d("TOKEN", "Updating: " + t.tokenInfo.chainId + (t.isEthereum()? " (Base Chain) ":"") + " : " + t.getAddress() + " : " + t.getFullName());
            balanceCheckDisposable = tokenRepository.updateTokenBalance(currentAddress, t.tokenInfo.chainId, t.getAddress(), t.getInterfaceSpec())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(balanceChange -> onBalanceChange(balanceChange, t.tokenInfo.chainId), this::onError);
        }

        if (System.currentTimeMillis() > nextOpenSeaCheck && focusToken == null) checkOpenSea();
        checkPendingChains();
    }

    public Single<BigDecimal> getChainBalance(String walletAddress, int chainId)
    {
        return tokenRepository.fetchChainBalance(walletAddress, chainId);
    }

    private void onBalanceChange(Boolean balanceChange, int chainId)
    {
        // could still be pending transactions so let's keep checking for a short while
        if (balanceChange && BuildConfig.DEBUG) Log.d("TOKEN", "Change Registered: * " + chainId);
    }

    private void checkPendingChains()
    {
        long currentTime = System.currentTimeMillis();
        Set<Integer> chainIds = pendingChainMap.keySet();
        for (Integer chainId : chainIds)
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
        openSeaCount++;
        nextOpenSeaCheck = System.currentTimeMillis() + OPENSEA_CHECK_INTERVAL;
        final Wallet wallet = new Wallet(currentAddress);
        NetworkInfo info = openSeaCount != OPENSEA_RINKEBY_CHECK ? ethereumNetworkRepository.getNetworkByChain(MAINNET_ID) : ethereumNetworkRepository.getNetworkByChain(RINKEBY_ID);
        if (BuildConfig.DEBUG) Log.d("OPENSEA", "Fetch from opensea : " + currentAddress + " : " + info.getShortName());
        tokenCheckDisposable = openseaService.getTokens(currentAddress, info.chainId, info.getShortName(), this)
                .flatMap(tokens -> tokenRepository.checkInterface(tokens, wallet)) //check the token interface
                .flatMap(tokens -> tokenRepository.storeTokens(wallet, tokens)) //store fetched tokens
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkERC20, this::onOpenseaError);

        if (openSeaCount >= OPENSEA_RINKEBY_CHECK) openSeaCount = 0;
    }

    private void checkERC20(Token[] checkedERC721Tokens)
    {
        if (erc20CheckDisposable == null || erc20CheckDisposable.isDisposed())
        {
            final String walletAddress = currentAddress;

            NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(MAINNET_ID);
            erc20CheckDisposable = tickerService.getTokensOnNetwork(info, walletAddress, this)
                    .flatMap(tokens -> tokenRepository.addERC20(new Wallet(walletAddress), tokens))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::finishCheckChain, this::onERC20Error);
        }
    }

    private void finishCheckChain(Token[] updatedMarketTokens)
    {
        erc20CheckDisposable = null;
    }

    private void onERC20Error(Throwable throwable)
    {
        erc20CheckDisposable = null;
        if (BuildConfig.DEBUG) throwable.printStackTrace();
    }

    private void onOpenseaError(Throwable throwable)
    {
        checkERC20(null);
    }

    public void updateTickers()
    {
        tickerService.updateTickers();
    }

    public Realm getRealmInstance(Wallet wallet)
    {
        return tokenRepository.getRealmInstance(wallet);
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
        startBalanceUpdate();
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
        Token highestToken = null;

        //this list will be in order of update.
        for (TokenCardMeta check : tokenList)
        {
            Token token = getToken(check.getChain(), check.getAddress());
            if (token == null) continue;
            long lastUpdateDiff = currentTime - check.lastUpdate;
            float weighting = check.calculateBalanceUpdateWeight();

            if (!appHasFocus && !token.isEthereum()) continue; //only check chains when wallet out of focus

            //simply multiply the weighting by the last diff.
            float updateFactor = weighting * (float) lastUpdateDiff;
            long cutoffCheck = 30*DateUtils.SECOND_IN_MILLIS; //normal minimum update frequency for token 30 seconds

            if (focusToken != null && token.tokenInfo.chainId == focusToken.chainId && token.getAddress().equalsIgnoreCase(focusToken.address))
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

    public void track(String gasSpeed)
    {
        AnalyticsProperties analyticsProperties = new AnalyticsProperties();
        analyticsProperties.setData(gasSpeed);

        analyticsService.track(C.AN_USE_GAS, analyticsProperties);
    }
}
