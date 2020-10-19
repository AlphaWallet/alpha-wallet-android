package com.alphawallet.app.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.entity.tokens.TokenUpdateEntry;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.token.entity.ContractAddress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.repository.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.app.repository.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.app.repository.EthereumNetworkBase.hasRealValue;

public class TokensService
{
    public static final String UNKNOWN_CONTRACT = "[Unknown Contract]";
    public static final String EXPIRED_CONTRACT = "[Expired Contract]";
    private static final long OPENSEA_CHECK_INTERVAL = 30 * DateUtils.SECOND_IN_MILLIS;
    private static final long OPENSEA_RINKEBY_CHECK = 4; //1 in [OPENSEA_RINKEBY_CHECK] opensea calls will to Rinkeby opensea
    public static final long PENDING_TIME_LIMIT = 3*DateUtils.MINUTE_IN_MILLIS; //cut off pending chain after 3 minutes

    private static final Map<String, Float> tokenValueMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<TokenUpdateEntry> tokenUpdateList = new ConcurrentLinkedQueue<>();
    private static final Map<Integer, Long> pendingChainMap = new ConcurrentHashMap<>();
    private static final Map<String, SparseArray<ContractType>> interfaceSpecMap = new ConcurrentHashMap<>();
    private String currentAddress = null;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;
    private final Context context;
    private final TickerService tickerService;
    private final OpenseaService openseaService;
    private final List<Integer> networkFilter;
    private ContractLocator focusToken;
    private final ConcurrentLinkedQueue<ContractAddress> unknownTokens;
    private long nextOpenSeaCheck;
    private int openSeaCount;
    private boolean walletInFocus = true;

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

    public TokensService(EthereumNetworkRepositoryType ethereumNetworkRepository,
                         TokenRepositoryType tokenRepository,
                         PreferenceRepositoryType preferenceRepository,
                         Context context,
                         TickerService tickerService,
                         OpenseaService openseaService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
        this.context = context;
        this.tickerService = tickerService;
        this.openseaService = openseaService;
        networkFilter = new ArrayList<>();
        setupFilter();
        focusToken = null;
        setCurrentAddress(preferenceRepository.getCurrentWalletAddress()); //set current wallet address at service startup
        this.unknownTokens = new ConcurrentLinkedQueue<>();
    }

    private void checkUnknownTokens()
    {
        if (queryUnknownTokensDisposable == null || queryUnknownTokensDisposable.isDisposed())
        {
            ContractAddress t = unknownTokens.poll();

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
        }
    }

    public Token getToken(int chainId, String addr)
    {
        if (TextUtils.isEmpty(currentAddress) || TextUtils.isEmpty(addr)) return null;
        else return tokenRepository.fetchToken(chainId, currentAddress, addr.toLowerCase());
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
            tokenUpdateList.clear();
            pendingChainMap.clear();
            if (eventTimer != null && !eventTimer.isDisposed())
            {
                eventTimer.dispose();
                eventTimer = null;
            }
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
            unknownTokens.add(cAddr);
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

    public void addToUpdateList(TokenCardMeta[] tokenMetas)
    {
        for (TokenCardMeta tm : tokenMetas)
        {
            if (tokenNotInList(tm))
            {
                TokenUpdateEntry t = new TokenUpdateEntry(tm.getChain(), tm.getAddress(), tm.type);
                t.lastUpdateTime = tm.lastUpdate;
                t.lastTxCheck = System.currentTimeMillis() - (t.isEthereum() ? 30000 : 0);
                t.balanceUpdateWeight = 2.0f + tm.calculateBalanceUpdateWeight();
                tokenUpdateList.add(t);
            }
        }

        addPopularTokens();
    }

    private void addPopularTokens()
    {
        // strategy for favourites:
        // 1. if enabled : always show
        // 2. if disabled but visibility not adjusted: check balance, show if non-zero
        // 3. if disabled and visibility adjusted: disable

        //now add any relevant contracts
        for (ContractLocator cl : ethereumNetworkRepository.getAllKnownContracts(getNetworkFilters()))
        {
            Token token = getToken(cl.chainId, cl.address);
            if (token != null)
            {
                TokenCardMeta meta = new TokenCardMeta(token);
                //if enabled or visibility not adjusted
                if (tokenNotInList(meta) && (tokenRepository.isEnabled(token) || !tokenRepository.hasVisibilityBeenChanged(token)))
                {
                    tokenUpdateList.add(new TokenUpdateEntry(meta.getChain(), meta.getAddress(), meta.type));
                }
            }
        }
    }

    private boolean tokenNotInList(TokenCardMeta tm)
    {
        int chainId = tm.getChain();
        String address = tm.getAddress();
        for (TokenUpdateEntry te : tokenUpdateList)
        {
            if (te.chainId == chainId && address.equalsIgnoreCase(te.tokenAddress))
            {
                return false;
            }
        }

        return true;
    }

    public void startBalanceUpdate()
    {
        nextOpenSeaCheck = System.currentTimeMillis() + 2*DateUtils.SECOND_IN_MILLIS; //delay first checking of Opensea/ERC20 to allow wallet UI to startup
        openSeaCount = 0;

        if (eventTimer != null && !eventTimer.isDisposed())
        {
            eventTimer.dispose();
        }

        if (balanceCheckDisposable != null && !balanceCheckDisposable.isDisposed()) balanceCheckDisposable.dispose();
        if (tokenCheckDisposable != null && !tokenCheckDisposable.isDisposed()) tokenCheckDisposable.dispose();
        if (erc20CheckDisposable != null && !erc20CheckDisposable.isDisposed()) erc20CheckDisposable.dispose();

        addUnresolvedContracts(ethereumNetworkRepository.getAllKnownContracts(getNetworkFilters()));

        eventTimer = Observable.interval(0, 500, TimeUnit.MILLISECONDS)
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
        TokenUpdateEntry t = getNextInBalanceUpdateQueue();

        if (t != null)
        {
            if (BuildConfig.DEBUG) Log.d("TOKEN", "Updating: " + t.chainId + (t.isEthereum()? " (Base Chain) ":"") + " : " + t.tokenAddress + " [" + t.balanceUpdateWeight + "]");
            if (t.balanceUpdateWeight > 2.0f) t.balanceUpdateWeight -= 2.0f; //reduce back to calculated amount
            balanceCheckDisposable = tokenRepository.updateTokenBalance(currentAddress, t.chainId, t.tokenAddress, t.type)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(balanceChange -> onBalanceChange(balanceChange, t.chainId), this::onError);
        }

        if (System.currentTimeMillis() > nextOpenSeaCheck && focusToken == null && walletInFocus) checkOpenSea();
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
            //mark tokens as checked
            updateCheckTime(checkedERC721Tokens);
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
        //mark tokens as checked
        updateCheckTime(updatedMarketTokens);
    }

    private void updateCheckTime(Token[] updatedTokens)
    {
        if (updatedTokens == null) return;
        long currentTime = System.currentTimeMillis();
        List<Token> checkedTokens = new ArrayList<>(Arrays.asList(updatedTokens));
        for (TokenUpdateEntry check : tokenUpdateList)
        {
            Token found = null;
            for (Token t : checkedTokens)
            {
                if (t.tokenInfo != null && t.tokenInfo.chainId == check.chainId && t.getAddress().equalsIgnoreCase(check.tokenAddress))
                {
                    found = t;
                    check.lastUpdateTime = currentTime;
                    check.balanceUpdateWeight = 0.25f; //token is updated from external source, reduce pressure
                    break;
                }
            }

            if (found != null) checkedTokens.remove(found);
        }
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
        walletInFocus = false;
    }

    public void walletShowing()
    {
        walletInFocus = true;
    }

    ///////////////////////////////////////////
    // Update Heuristics - timings and weightings for token updates
    // Fine tune how and when tokens are updated here

    /**
     * Token update heuristic - calculates which token should be updated next
     * @return
     */
    public TokenUpdateEntry getNextInBalanceUpdateQueue()
    {
        //calculate update based on last update time & importance
        float highestWeighting = 0;
        long currentTime = System.currentTimeMillis();
        TokenUpdateEntry highestToken = null;

        for (TokenUpdateEntry check : tokenUpdateList)
        {
            long lastUpdateDiff = currentTime - check.lastUpdateTime;
            float weighting = check.balanceUpdateWeight;

            if (!walletInFocus && !check.isEthereum()) continue; //only check chains when wallet out of focus

            //simply multiply the weighting by the last diff.
            float updateFactor = weighting * (float) lastUpdateDiff;
            long cutoffCheck = 30*DateUtils.SECOND_IN_MILLIS; //normal minimum update frequency for token 30 seconds

            if (focusToken != null && check.chainId == focusToken.chainId && check.tokenAddress.equalsIgnoreCase(focusToken.address))
            {
                updateFactor = 3.0f * (float) lastUpdateDiff;
                cutoffCheck = 15*DateUtils.SECOND_IN_MILLIS; //focus token can be checked every 15 seconds - focus token when erc20 or chain clicked on in wallet
            }
            else if (check.isEthereum() && pendingChainMap.containsKey(check.chainId)) //higher priority for checking balance of pending chain
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
                highestToken = check;
            }
        }

        if (highestToken != null)
        {
            highestToken.lastUpdateTime = System.currentTimeMillis();
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
        //calculate update based on last update time & importance
        long currentTime = System.currentTimeMillis();
        TokenUpdateEntry highestToken = null;
        long highestDiff = 0;

        for (TokenUpdateEntry check : tokenUpdateList)
        {
            if (!check.needsTransactionCheck() || !walletInFocus) continue;
            long timeIntervalCheck = getTokenTimeInterval(check, pendingTxChains);
            if (timeIntervalCheck == 0) continue;

            if (focusToken != null && check.chainId == focusToken.chainId)
            {
                timeIntervalCheck = 10*DateUtils.SECOND_IN_MILLIS;
            }

            if (currentTime >= (check.lastTxCheck + timeIntervalCheck))
            {
                long diff = currentTime - (check.lastTxCheck + timeIntervalCheck);
                if (diff > highestDiff)
                {
                    highestDiff = diff;
                    highestToken = check;
                }
            }
        }

        if (highestToken != null)
        {
            highestToken.lastTxCheck = currentTime;
            return getToken(highestToken.chainId, highestToken.tokenAddress);
        }

        return null;
    }

    /**
     * Timings for when there can be a check for new transactions
     * @param t
     * @param pending
     * @return
     */
    private long getTokenTimeInterval(TokenUpdateEntry t, Collection<Integer> pending)
    {
        long nextTimeCheck;

        Token token = getToken(t.chainId, t.tokenAddress);

        if (t.isEthereum() && pending != null && pending.contains(t.chainId)) //check chain every 10 seconds while transaction is pending
        {
            nextTimeCheck = 10*DateUtils.SECOND_IN_MILLIS;
        }
        else if (t.isEthereum())
        {
            nextTimeCheck = 30*DateUtils.SECOND_IN_MILLIS; //allow base chains to be checked about every 30 seconds when not pending
        }
        else if (token != null)
        {
            nextTimeCheck = token.getTransactionCheckInterval();
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
}
