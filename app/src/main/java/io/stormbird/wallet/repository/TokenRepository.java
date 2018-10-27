package io.stormbird.wallet.repository;

import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;

import io.reactivex.disposables.CompositeDisposable;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.SubscribeWrapper;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenFactory;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.TokenTicker;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionOperation;
import io.stormbird.wallet.entity.TransferFromEventResponse;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TickerService;
import io.stormbird.wallet.service.TokenExplorerClientType;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSyncing;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.BadContract;
import rx.functions.Func1;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TokenRepository implements TokenRepositoryType {

    private static final String TAG = "TRT";
    private static final long BALANCE_UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS;
    private final TokenExplorerClientType tokenNetworkService;
    private final WalletRepositoryType walletRepository;
    private final TokenLocalSource localSource;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionLocalSource transactionsLocalCache;
    private final AssetDefinitionService assetDefinitionService;
    private final TickerService tickerService;
    private Web3j web3j;
    private boolean useBackupNode = false;
    private NetworkInfo network;
    private Disposable disposable;

    public TokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            WalletRepositoryType walletRepository,
            TokenExplorerClientType tokenNetworkService,
            TokenLocalSource localSource,
            TransactionLocalSource transactionsLocalCache,
            TickerService tickerService,
            AssetDefinitionService assetDefinitionService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.walletRepository = walletRepository;
        this.tokenNetworkService = tokenNetworkService;
        this.localSource = localSource;
        this.transactionsLocalCache = transactionsLocalCache;
        this.tickerService = tickerService;
        this.ethereumNetworkRepository.addOnChangeDefaultNetwork(this::buildWeb3jClient);
        this.assetDefinitionService = assetDefinitionService;
        buildWeb3jClient(ethereumNetworkRepository.getDefaultNetwork());
    }

    private void buildWeb3jClient(NetworkInfo defaultNetwork)
    {
        network = defaultNetwork;
        org.web3j.protocol.http.HttpService publicNodeService = new org.web3j.protocol.http.HttpService(defaultNetwork.rpcServerUrl);
        web3j = Web3jFactory.build(publicNodeService);
        ethereumNetworkRepository.setActiveRPC(defaultNetwork.rpcServerUrl);

        //test main node, if it's not working then use backup Infura node. If it's not working then we can't listen on the pool
        disposable = getIsSyncing()
                .subscribeOn(Schedulers.io())
                .subscribe(this::receiveSyncing, this::checkFail);
    }

    private void receiveSyncing(Boolean b)
    {
        //have a valid connection and node is done syncing, no need to use infura
        if (b == null || b)
        {
            useBackupNode = true;
            switchToBackupNode();
        }

        disposable.dispose();
    }

    private void checkFail(Throwable failMsg)
    {
        useBackupNode = true;
        switchToBackupNode();
        disposable.dispose();
    }

    private void switchToBackupNode()
    {
        org.web3j.protocol.http.HttpService publicNodeService = new org.web3j.protocol.http.HttpService(network.backupNodeUrl);
        web3j = Web3jFactory.build(publicNodeService);
        ethereumNetworkRepository.setActiveRPC(network.backupNodeUrl);
    }

    private Single<Boolean> getIsSyncing()
    {
        return Single.fromCallable(() -> {
            EthSyncing status = web3j.ethSyncing()
                    .send();
            return status.isSyncing();
        });
    }

    @Override
    public Observable<Token[]> fetchActive(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchCachedEnabledTokens(network, wallet).toObservable();
    }

    @Override
    public Observable<Token[]> fetchActiveCache(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchCachedEnabledTokens(network, wallet) // Immediately show the cache.
                .toObservable();
    }

    @Override
    public Observable<Token[]> fetchActiveStoredPlusEth(NetworkInfo network, String walletAddress) {
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokens(network, wallet) // fetch tokens from cache
                .compose(attachEthereumStored(network, wallet)) //add cached eth balance
                .compose(attachERC721Tokens(wallet))
                .toObservable();
    }

    /**
     * Gives an observable that allows us to process each token as the balance is fetched
     *
     * @param walletAddress
     * @return
     */
    @Override
    public Observable<Token> fetchActiveStoredSequential(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokensList(network, wallet)
                .compose(attachEthereumActive(network, wallet))
                .flatMapIterable(tokens -> tokens)
                .flatMap(token -> processBalance(network, wallet, token));
    }

    @Override
    public Observable<Token> fetchActiveStoredSequentialNoEth(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokensList(network, wallet)
                .flatMapIterable(tokens -> tokens)
                .flatMap(token -> processBalance(network, wallet, token));
    }

    //Add in the fetched current ethereum balance
    private ObservableTransformer<List<Token>, List<Token>> attachEthereumActive(NetworkInfo network, Wallet wallet)
    {
        return upstream -> Observable.zip(
                upstream, attachEth(network, wallet).toObservable(),
                (tokens, ethToken) ->
                {
                    List<Token> result = new ArrayList<>();
                    result.add(ethToken);
                    result.addAll(tokens);
                    return result;
                });
    }

    private Observable<Token> processBalance(NetworkInfo network, Wallet wallet, Token token)
    {
        //now fetch the balance
        return updateBalance(network, wallet, token)
                .observeOn(Schedulers.newThread())
                .toObservable();
    }

    private Single<Token[]> fetchERC721Tokens(Wallet wallet)
    {
        return localSource.fetchERC721Tokens(wallet);
    }

    private Observable<List<Token>> fetchStoredEnabledTokensList(NetworkInfo network, Wallet wallet) {
        return localSource
                .fetchEnabledTokensSequentialList(network, wallet);
    }

    @Override
    public Observable<Token[]> fetchActiveStored(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokens(network, wallet) // fetch tokens from cache
                .toObservable();
    }

    private SingleTransformer<Token[], Token[]> attachERC721Tokens(Wallet wallet)
    {
        return upstream -> Single.zip(
                upstream, fetchERC721Tokens(wallet),
                (tokens, ERC721Tokens) ->
                {
                    List<Token> result = new ArrayList<>();
                    result.addAll(Arrays.asList(ERC721Tokens));
                    result.addAll(Arrays.asList(tokens));
                    return result.toArray(new Token[result.size()]);
                });
    }

    private SingleTransformer<Token[], Token[]> attachEthereumStored(NetworkInfo network, Wallet wallet)
    {
        return upstream -> Single.zip(
                upstream, attachCachedEth(network, wallet),
                (tokens, ethToken) ->
                {
                    List<Token> result = new ArrayList<>();
                    result.add(ethToken);
                    result.addAll(Arrays.asList(tokens));
                    return result.toArray(new Token[result.size()]);
                });
    }

//    private Single<Token> attachCachedEth(NetworkInfo network, Wallet wallet) {
//        return walletRepository.balanceInWei(wallet)
//                .map(balance -> {
//                    TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true);
//                    Token eth = new Token(info, balance, System.currentTimeMillis());
//                    eth.setIsEthereum();
//                    return eth;
//                });
//    }

    private Single<Token> attachCachedEth(NetworkInfo network, Wallet wallet)
    {
        //get stored eth balance
        return Single.fromCallable(() -> {
            Token eth = localSource.getTokenBalance(network, wallet, wallet.address);
            if (eth == null)
            {
                TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true);
                BigDecimal balance = BigDecimal.ZERO;
                eth = new Token(info, balance, System.currentTimeMillis());
                eth.setTokenNetwork(network.chainId);
                eth.setTokenWallet(wallet.address);
            }
            eth.setIsEthereum();
            return eth;
        });
    }

    @Override
    public Observable<Token> fetchActiveSingle(String walletAddress, Token token)
    {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return Single.merge(
                fetchCachedToken(network, wallet, token.getAddress()),
                updateBalance(network, wallet, token)) // Looking for new tokens
                .toObservable();
    }

    @Override
    public Observable<Token> fetchCachedSingleToken(String walletAddress, String tokenAddress)
    {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchCachedToken(network, wallet, tokenAddress)
                .toObservable();
    }

    /**
     * Just updates the balance of a token
     *
     * @param walletAddress
     * @param token
     * @return
     */
    @Override
    public Observable<Token> fetchActiveTokenBalance(String walletAddress, Token token)
    {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return updateBalance(network, wallet, token)
                .observeOn(Schedulers.newThread())
                .toObservable();
    }

    @Override
    public Observable<Token> fetchActiveDefaultTokenBalance(Token token)
    {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        return walletRepository.getDefaultWallet()
                .flatMap(wallet -> updateBalance(network, wallet, token))
                .observeOn(Schedulers.newThread())
                .toObservable();
    }

    @Override
    public Observable<Token> fetchActiveTokenBalance(Token token, NetworkInfo network, Wallet wallet)
    {
        return updateBalance(network, wallet, token)
                .observeOn(Schedulers.newThread())
                .toObservable();
    }

    private Observable<Token> fetchBalance(String walletAddress, Token token)
    {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return updateBalance(network, wallet, token)
                .observeOn(Schedulers.newThread())
                .toObservable();
    }

    @Override
    public Observable<Token[]> fetchAll(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return localSource.fetchAllTokens(network, wallet).toObservable();
    }

    private SingleTransformer<Token[], Token[]> attachTicker(NetworkInfo network, Wallet wallet) {
        return upstream -> upstream.flatMap(tokens ->
                Single.zip(
                        Single.just(tokens),
                        getTickers(network, wallet, tokens),
                        (data, tokenTickers) -> {
                            for (Token token : data) {
                                for (TokenTicker ticker : tokenTickers) {
                                    if (token.tokenInfo.address.equals(ticker.contract)) {
                                        token.ticker = ticker;
                                    }
                                }
                            }
                            return data;
                        }));
    }

    private SingleTransformer<Token[], Token[]> attachTickerStored(NetworkInfo network, Wallet wallet) {
        return upstream -> upstream.flatMap(tokens ->
                Single.zip(
                        Single.just(tokens),
                        getTickersStored(network, wallet, tokens),
                        (data, tokenTickers) -> {
                            for (Token token : data) {
                                for (TokenTicker ticker : tokenTickers) {
                                    if (token.tokenInfo.address.equals(ticker.contract)) {
                                        token.ticker = ticker;
                                    }
                                }
                            }
                            return data;
                        }));
    }

    private Single<TokenTicker[]> getTickersStored(NetworkInfo network, Wallet wallet, Token[] tokens) {
        return localSource.fetchTickers(network, wallet, tokens);
    }

    private Single<TokenTicker[]> getTickers(NetworkInfo network, Wallet wallet, Token[] tokens) {
        return localSource.fetchTickers(network, wallet, tokens)
                .onErrorResumeNext(throwable -> tickerService
                        .fetchTokenTickers(tokens, "USD")
                        .onErrorResumeNext(thr -> Single.just(new TokenTicker[0])))
                .flatMapCompletable(tokenTickers -> localSource.saveTickers(network, wallet, tokenTickers))
                .andThen(localSource
                        .fetchTickers(network, wallet, tokens)
                        .onErrorResumeNext(thr -> Single.just(new TokenTicker[0])));
    }

    @Override
    public Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo) {
        TokenFactory tf = new TokenFactory();
        Token newToken = tf.createToken(tokenInfo);
        newToken.setTokenWallet(wallet.address);
        newToken.setTokenNetwork(ethereumNetworkRepository.getDefaultNetwork().chainId);
        Log.d(TAG, "Create for store3: " + tokenInfo.name);

        return localSource.saveToken(
                    ethereumNetworkRepository.getDefaultNetwork(),
                    wallet,
                    newToken);
    }

    @Override
    public Single<Token[]> addERC721(Wallet wallet, Token[] tokens)
    {
        return localSource.saveERC721Tokens(
                ethereumNetworkRepository.getDefaultNetwork(),
                wallet,
                tokens);
    }

    @Override
    public Single<Token[]> addTokens(Wallet wallet, TokenInfo[] tokenInfos)
    {
        TokenFactory tf = new TokenFactory();
        Token[] tokenList = new Token[tokenInfos.length];

        for (int i = 0; i < tokenInfos.length; i++)
        {
            tokenList[i] = tf.createToken(tokenInfos[i]);
            Log.d(TAG, "Create for store: " + tokenInfos[i].name);
        }

        return localSource.saveTokensList(
                    ethereumNetworkRepository.getDefaultNetwork(),
                    wallet,
                    tokenList);
    }

    @Override
    public Completable setEnable(Wallet wallet, Token token, boolean isEnabled) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        return Completable.fromAction(() -> localSource.setEnable(network, wallet, token, isEnabled));
    }

    @Override
    public Completable setBurnList(Wallet wallet, Token token, List<Integer> burnList) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        return Completable.fromAction(() -> localSource.updateTokenBurn(network, wallet, token, burnList));
    }

    @Override
    public Observable<TokenInfo> update(String contractAddr) {
        return setupTokensFromLocal(contractAddr).toObservable();
    }

    @Override
    public void terminateToken(Token token, Wallet wallet, NetworkInfo network)
    {
        localSource.setTokenTerminated(network, wallet, token);
    }

    @Override
    public Single<TokenInfo[]> update(String[] address)
    {
        return setupTokensFromLocal(address);
    }

    private Single<Token[]> fetchFromNetworkSource(@NonNull NetworkInfo network, @NonNull Wallet wallet) {
        return Single.fromCallable(() -> {
            try {
                return network.isMainNetwork
                        ? tokenNetworkService.fetch(wallet.address).blockingFirst()
                        : new TokenInfo[0];
            } catch (Throwable th) {
                // Ignore all errors, it's not important source.
                return new TokenInfo[0];
            }
        })
        .map(this::mapToTokens);
    }

    private Single<Token[]> extractFromTransactions(NetworkInfo network, Wallet wallet) {
        return transactionsLocalCache.fetchTransaction(network, wallet)
                .flatMap(transactions -> {
                    List<Token> result = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        if (transaction.operations == null || transaction.operations.length == 0) {
                            continue;
                        }
                        TransactionOperation operation = transaction.operations[0];
                        result.add(new Token(new TokenInfo(
                                operation.contract.address,
                                operation.contract.name,
                                operation.contract.symbol,
                                operation.contract.decimals,
                                true), null, 0));
                    }
                    return Single.just(result.toArray(new Token[result.size()]));
                });
    }

    private Completable updateTokens(NetworkInfo network, Wallet wallet) {
        return Single.zip(
                fetchFromNetworkSource(network, wallet),
                extractFromTransactions(network, wallet),
                localSource.fetchAllTokens(network, wallet),
                (fromNetTokens, fromTrxTokens, cachedTokens) -> {
                    final Set<String> oldTokensIndex = new HashSet<>();
                    final List<Token> zip = new ArrayList<>();
                    zip.addAll(Arrays.asList(fromNetTokens));
                    zip.addAll(Arrays.asList(fromTrxTokens));
                    final List<Token> newTokens = new ArrayList<>();
                    for (Token cachedToken : cachedTokens) {
                        oldTokensIndex.add(cachedToken.tokenInfo.address);
                    }
                    for (int i = zip.size() - 1; i > -1; i--) {
                        if (!oldTokensIndex.contains(zip.get(i).tokenInfo.address)) {
                            newTokens.add(zip.get(i));
                        }
                    }
                    return newTokens.toArray(new Token[newTokens.size()]);
                })
                .flatMapCompletable(tokens -> localSource.saveTokens(network, wallet, tokens));
    }

    /**
     * Obtain live balance of token from Ethereum blockchain and cache into Realm
     *
     * @param network
     * @param wallet
     * @param token
     * @return
     */
    private Single<Token> updateBalance(NetworkInfo network, Wallet wallet, final Token token) {
        if (token.isEthereum())
        {
            return attachEth(network, wallet);
        }
        else
        return Single.fromCallable(() -> {
            TokenFactory tFactory = new TokenFactory();
            try
            {
                List<BigInteger> balanceArray = null;
                List<Integer> burnArray = null;
                BigDecimal balance = null;
                TokenInfo tInfo = token.tokenInfo;
                if (token.tokenInfo.isStormbird)
                {
                    Ticket t = (Ticket) token;
                    balanceArray = getBalanceArray(wallet, t.tokenInfo);
                    if (balanceArray.size() == 1 && balanceArray.get(0).equals(BigInteger.valueOf(-1))) //when there's an index out of bounds fail - caused by trying to read ERC20 balance as an array
                    {
                        tInfo = changeToERC20Token(token);
                        balance = BigDecimal.ZERO;
                        balanceArray = new ArrayList<>();
                    }
                    else
                    {
                        burnArray = t.getBurnList();
                    }
                }
                else
                {
                    balance = getBalance(wallet, token.tokenInfo);
                    if (balance.equals(BigDecimal.valueOf(-1)))
                    {
                        balance = BigDecimal.ZERO;
                        //array balance, assume ERC875
                        tInfo = changeToERC875Token(token);
                        balanceArray = new ArrayList<>(Arrays.asList(BigInteger.ZERO));
                    }
                }

                Token updated = tFactory.createToken(tInfo, balance, balanceArray, burnArray, System.currentTimeMillis());
                localSource.updateTokenBalance(network, wallet, updated);
                updated.setTokenWallet(wallet.address);
                updated.setTokenNetwork(network.chainId);
                return updated;
            }
            catch (BadContract e)
            {
                return token;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return token;
            }
        });
    }

    private ObservableTransformer<Token, Token> updateBalance(NetworkInfo network, Wallet wallet) {
        return upstream -> upstream.map(token -> {
            TokenFactory tFactory = new TokenFactory();
            long now = System.currentTimeMillis();
            long minUpdateBalanceTime = now - BALANCE_UPDATE_INTERVAL;
            BigDecimal balance = null;
            List<BigInteger> balanceArray = null;
            List<Integer> burnArray = null;
            TokenInfo tInfo = token.tokenInfo;
            if (token.balance == null || token.updateBlancaTime < minUpdateBalanceTime) {
                try {
                    if (token.tokenInfo.isStormbird)
                    {
                        balanceArray = getBalanceArray(wallet, token.tokenInfo);
                        if (balanceArray.size() == 1 && balanceArray.get(0).equals(BigInteger.valueOf(-1)))
                        {
                            tInfo = changeToERC20Token(token);
                            burnArray = null;
                        }
                        else
                        {
                            burnArray = ((Ticket) token).getBurnList();
                        }
                    }
                    else
                    {
                        balance = getBalance(wallet, token.tokenInfo);
                        if (balance.equals(BigDecimal.valueOf(-1)))
                        {
                            balance = BigDecimal.ZERO;
                            tInfo = changeToERC875Token(token);
                            balanceArray = new ArrayList<>(Arrays.asList(BigInteger.ZERO));
                        }
                    }
                    token = tFactory.createToken(tInfo, balance, balanceArray, burnArray, now);
                    localSource.updateTokenBalance(network, wallet, token);
                } catch (Throwable th) { /* Quietly */ }
            }
            return token;
        });
    }

    private TokenInfo changeToERC20Token(Token token)
    {
        return new TokenInfo(token.tokenInfo.address,
                                        token.tokenInfo.name,
                                        token.tokenInfo.symbol,
                                        token.tokenInfo.decimals,
                                        true, false);
    }

    private TokenInfo changeToERC875Token(Token token)
    {
        return new TokenInfo(token.tokenInfo.address, token.tokenInfo.name, token.tokenInfo.symbol, token.tokenInfo.decimals, true, true);
    }

    private SingleTransformer<Token[], Token[]> attachEthereum(NetworkInfo network, Wallet wallet)
    {
        return upstream -> Single.zip(
                upstream, attachEth(network, wallet),
                (tokens, ethToken) ->
                {
                    List<Token> result = new ArrayList<>();
                    result.add(ethToken);
                    result.addAll(Arrays.asList(tokens));
                    return result.toArray(new Token[result.size()]);
                });
    }

    @Override
    public rx.Subscription memPoolListener(SubscribeWrapper subscriber)
    {
        if (!useBackupNode)
        {
            return web3j.pendingTransactionObservable()
                    .subscribeOn(rx.schedulers.Schedulers.newThread())
                    .subscribe(subscriber.scanReturn, subscriber.onError);
        }
        else
        {
            return null;
        }
    }

    private Single<Token[]> fetchStoredEnabledTokens(NetworkInfo network, Wallet wallet) {
        return localSource
                .fetchEnabledTokensWithBalance(network, wallet);
    }

    private Single<Token[]> fetchCachedEnabledTokens(NetworkInfo network, Wallet wallet) {
        return localSource
                .fetchEnabledTokens(network, wallet)
                .flatMapObservable(Observable::fromArray)
                .compose(updateBalance(network, wallet))
                .toList()
                .map(list -> list.toArray(new Token[list.size()]))
                .compose(attachTicker(network, wallet))
                .compose(attachEthereum(network, wallet));
    }

    private Single<Token> fetchCachedToken(NetworkInfo network, Wallet wallet, String address) {
        return localSource
                .fetchEnabledToken(network, wallet, address);
    }

    private Single<Token> attachEth(NetworkInfo network, Wallet wallet) {
        return walletRepository.balanceInWei(wallet)
                .map(balance -> {
                    if (balance.equals(BigDecimal.valueOf(-1)))
                    {
                        //network error - retrieve from cache
                        Token b = localSource.getTokenBalance(network, wallet, wallet.address);
                        if (b != null) balance = b.balance;
                        else balance = BigDecimal.ZERO;
                    }
                    TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true);
                    Token eth = new Token(info, balance, System.currentTimeMillis());
                    eth.setIsEthereum();
                    eth.setTokenNetwork(network.chainId);
                    eth.setTokenWallet(wallet.address);
                    //store token and balance
                    localSource.updateTokenBalance(network, wallet, eth);
                    return eth;
                })
                .flatMap(token -> ethereumNetworkRepository.getTicker()
                        .map(ticker -> {
                            token.ticker = new TokenTicker("", "", ticker.price_usd, ticker.percentChange24h, null);
                            return token;
                        }).onErrorResumeNext(throwable -> Single.just(token)));
    }

    /**
     * Either returns the live eth balance or cached if network is unavilable
     * We can derive the time when the balance was fetched from the Token info
     *
     * @param network
     * @param wallet
     * @return
     */
    @Override
    public Single<Token> getEthBalance(NetworkInfo network, Wallet wallet) {
        return walletRepository.balanceInWei(wallet)
                .map(balance -> {
                    if (balance.equals(BigDecimal.valueOf(-1)))
                    {
                        //network error - retrieve from cache
                        Token b = localSource.getTokenBalance(network, wallet, wallet.address);
                        if (b != null) balance = b.balance;
                        else balance = BigDecimal.ZERO;
                    }
                    TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true);
                    Token eth = new Token(info, balance, System.currentTimeMillis());
                    eth.setIsEthereum();
                    //store token and balance
                    localSource.updateTokenBalance(network, wallet, eth);
                    return eth;
                });
    }

    @Override
    public Single<Ticker> getEthTicker()
    {
        return ethereumNetworkRepository.getTicker();
    }

    private BigDecimal getBalance(Wallet wallet, TokenInfo tokenInfo) throws Exception {
        Function function = balanceOf(wallet.address);
        String responseValue = callSmartContractFunction(function, tokenInfo.address, wallet);

        if (responseValue == null) return BigDecimal.ZERO;

        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            BigDecimal retVal = new BigDecimal(((Uint256) response.get(0)).getValue());
            if (retVal.equals(BigDecimal.valueOf(32)))
            {
                return checkValue(retVal, wallet, responseValue);
            }
            else
            {
                return retVal;
            }
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * This function checks for suspicious contracts. If the value of tokens is 32, this could be an array return.
     * Parsing the returned value with DynamicArray, if it is an array spec (ie 0x02, then array size etc) then
     * we can remove this token. However it would be better to display the token as a ticket if the array balance has elements.
     * @param value
     * @param wallet
     * @param responseValue
     * @return
     */
    private BigDecimal checkValue(BigDecimal value, Wallet wallet, String responseValue)
    {
        org.web3j.abi.datatypes.Function functionCheck = balanceOfArray(wallet.address);
        List<Type> values = FunctionReturnDecoder.decode(responseValue, functionCheck.getOutputParameters());
        //is it an array?
        if (values.isEmpty()) return value;
        else if (values.size() > 0)
        {
            return BigDecimal.valueOf(-1);
        }
        else
        {
            return value;
        }
    }

    private List<BigInteger> getBalanceArray(Wallet wallet, TokenInfo tokenInfo) throws Exception {
        List<BigInteger> result = new ArrayList<>();
        byte[] temp = new byte[16];
        try
        {
            if (tokenInfo.isStormbird) //safety check
            {
                org.web3j.abi.datatypes.Function function = balanceOfArray(wallet.address);
                List<Bytes32> indices = callSmartContractFunctionArray(function, tokenInfo.address, wallet);
                if (indices == null) return result; // return empty array
                for (Bytes32 val : indices)
                {
                    result.add(getCorrectedValue(val, temp));
                }
            }
        }
        catch (StringIndexOutOfBoundsException e)
        {
            //This is probably caused by trying to read the ERC875 balance of an ERC20
            result.add(BigInteger.valueOf(-1));
        }
        return result;
    }

    public rx.Observable<TransferFromEventResponse> burnListenerObservable(String contractAddr)
    {
        if (!useBackupNode)
        {
            final Event event = new Event("TransferFrom",
                                          Arrays.<TypeReference<?>>asList(new TypeReference<Address>()
                                          {
                                          }, new TypeReference<Address>()
                                          {
                                          }),
                                          Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint16>>()
                                          {
                                          }));
            EthFilter filter = new EthFilter(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.PENDING, contractAddr);
            filter.addSingleTopic(EventEncoder.encode(event));
            return web3j.ethLogObservable(filter).map(new Func1<org.web3j.protocol.core.methods.response.Log, TransferFromEventResponse>()
            {
                @Override
                public TransferFromEventResponse call(org.web3j.protocol.core.methods.response.Log log)
                {
                    EventValues eventValues = extractEventParameters(event, log);
                    TransferFromEventResponse typedResponse = new TransferFromEventResponse();
                    typedResponse._from = (String) eventValues.getIndexedValues().get(0).getValue();
                    typedResponse._to = (String) eventValues.getIndexedValues().get(1).getValue();
                    typedResponse._indices = (List<Uint16>) eventValues.getNonIndexedValues().get(0).getValue();
                    return typedResponse;
                }
            });
        }
        else
        {
            return rx.Observable.fromCallable(() -> {
                TransferFromEventResponse event = new TransferFromEventResponse();
                event._from = "";
                event._to = "";
                event._indices = null;
                return event;
            });
        }
    }

    /**
     * checking if we need to read a top 16 byte value specifically
     * We should keep this function in here because when we start to use 32 byte values there is
     * potentially a problem with the 'always move 16 bytes to low 16' force solution.
     *
     * A better solution is not to fight this ethereum feature - we simply start interpreting the XML from
     * the top byte.
     */
    private BigInteger getCorrectedValue(Bytes32 val, byte[] temp)
    {
        BigInteger retVal;
        //does the top second byte have a value and the lower 16 bytes are zero?
        long lowCheck = 0;
        long highCheck = val.getValue()[0] + val.getValue()[1];
        for (int i = 16; i < 32; i++) lowCheck += val.getValue()[i];
        if (highCheck != 0 && lowCheck == 0)
        {
            System.arraycopy(val.getValue(), 0, temp, 0, 16);
            retVal = Numeric.toBigInt(temp);
        }
        else
        {
            retVal = Numeric.toBigInt(val.getValue());
        }

        return retVal;
    }

    private <T> T getContractData(String address, org.web3j.abi.datatypes.Function function) throws Exception
    {
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);

        if (responseValue == null) return null;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1)
        {
            return (T) response.get(0).getValue();
        }
        else
        {
            return null;
        }
    }

    private String getName(String address) throws Exception {
        org.web3j.abi.datatypes.Function function = nameOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);

        if (responseValue == null) return null;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            String name = (String)response.get(0).getValue();
            if (assetDefinitionService.getNetworkId(address) > 0)
            {
                name = name + " " + assetDefinitionService.getAssetDefinition(address).getTokenName();
            }
            return name;
        } else {
            return null;
        }
    }

    private String getSymbol(String address) throws Exception {
        org.web3j.abi.datatypes.Function function = symbolOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);

        if (responseValue == null) return null;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return (String)response.get(0).getValue();
        } else {
            return null;
        }
    }

    private String getVenue(String address) throws Exception {
        org.web3j.abi.datatypes.Function function = symbolOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);

        if (responseValue == null) return null;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return (String)response.get(0).getValue();
        } else {
            return null;
        }
    }

    private int getDecimals(String address) throws Exception {
        org.web3j.abi.datatypes.Function function = decimalsOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);
        if (responseValue == null) return 18;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return ((Uint8) response.get(0)).getValue().intValue();
        } else {
            return 18;
        }
    }

    private static org.web3j.abi.datatypes.Function balanceOf(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private static org.web3j.abi.datatypes.Function balanceOfArray(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Bytes32>>() {}));
    }

    private static org.web3j.abi.datatypes.Function nameOf() {
        return new Function("name",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static org.web3j.abi.datatypes.Function stringParam(String param) {
        return new Function(param,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static org.web3j.abi.datatypes.Function boolParam(String param) {
        return new Function(param,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
    }

    private static org.web3j.abi.datatypes.Function intParam(String param) {
        return new Function(param,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint>() {}));
    }

    private static org.web3j.abi.datatypes.Function symbolOf() {
        return new Function("symbol",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static org.web3j.abi.datatypes.Function decimalsOf() {
        return new Function("decimals",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    }

    private List callSmartContractFunctionArray(
            org.web3j.abi.datatypes.Function function, String contractAddress, Wallet wallet) throws Exception
    {
        String encodedFunction = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.response.EthCall ethCall = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction
                        .createEthCallTransaction(wallet.address, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        String value = ethCall.getValue();
        List<Type> values = FunctionReturnDecoder.decode(value, function.getOutputParameters());
        if (values.isEmpty()) return null;

        Type T = values.get(0);
        Object o = T.getValue();
        return (List) o;
    }

    private String callSmartContractFunction(
            Function function, String contractAddress, Wallet wallet) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(wallet.address, contractAddress, encodedFunction);
            EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] createTokenTransferData(String to, BigInteger tokenAmount) {
        List<Type> params = Arrays.asList(new Address(to), new Uint256(tokenAmount));
        List<TypeReference<?>> returnTypes = Collections.singletonList(new TypeReference<Bool>() {});
        Function function = new Function("transfer", params, returnTypes);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    private static Function getTransferFunction(String to, List<BigInteger> ticketIndices)
    {
        Function function = new Function(
                "transfer",
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(to),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint16>(
                                org.web3j.abi.Utils.typeMap(ticketIndices, org.web3j.abi.datatypes.generated.Uint16.class))),
                Collections.<TypeReference<?>>emptyList());
        return function;
    }

    public static byte[] createTicketTransferData(String to, String indexListStr) {
        //params are: Address, List<Uint16> of ticket indicies
        List ticketIndicies = Observable.fromArray(indexListStr.split(","))
                .map(s -> new BigInteger(s.trim())).toList().blockingGet();
        Function function = getTransferFunction(to, ticketIndicies);

        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createTrade(BigInteger expiry, List<BigInteger> ticketIndices, int v, byte[] r, byte[] s)
    {
        Function function = getTradeFunction(expiry, ticketIndices, v, r, s);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    private static Function getTradeFunction(BigInteger expiry, List<BigInteger> ticketIndices, int v, byte[] r, byte[] s)
    {
        Function function = new Function(
                "trade",
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(expiry),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint16>(
                                org.web3j.abi.Utils.typeMap(ticketIndices, org.web3j.abi.datatypes.generated.Uint16.class)),
                        new org.web3j.abi.datatypes.generated.Uint8(v),
                        new org.web3j.abi.datatypes.generated.Bytes32(r),
                        new org.web3j.abi.datatypes.generated.Bytes32(s)),
                Collections.<TypeReference<?>>emptyList());
        return function;
    }

    private Token[] mapToTokens(TokenInfo[] items) {
        int len = items.length;
        Token[] tokens = new Token[len];
        for (int i = 0; i < len; i++) {
            tokens[i] = new Token(items[i], null, 0);
        }
        return tokens;
    }

    private Single<TokenInfo> setupTokensFromLocal(String address)
    {
        return Single.fromCallable(() -> {
            try
            {
                long now = System.currentTimeMillis();
                Boolean isStormbird = getContractData(address, boolParam("isStormBirdContract"));
                if (isStormbird == null) isStormbird = false;
                TokenInfo result = new TokenInfo(
                        address,
                        getName(address),
                        getContractData(address, stringParam("symbol")),
                        getDecimals(address),
                        true,
                        isStormbird);

                return result;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        });
    }

    private Single<TokenInfo[]> setupTokensFromLocal(String[] addresses)
    {
        return Single.fromCallable(() -> {
            List<TokenInfo> tokenList = new ArrayList<>();
            try
            {
                for (String address : addresses)
                {
                    long now = System.currentTimeMillis();
                    String name = getName(address);
                    Boolean isStormbird = getContractData(address, boolParam("isStormBirdContract"));
                    if (isStormbird == null) isStormbird = false;
                    TokenInfo result = new TokenInfo(
                            address,
                            name,
                            getContractData(address, stringParam("symbol")),
                            getDecimals(address),
                            true,
                            isStormbird);

                    tokenList.add(result);
                }
                return tokenList.toArray(new TokenInfo[tokenList.size()]);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return tokenList.toArray(new TokenInfo[tokenList.size()]);
            }
        });
    }

    protected EventValues extractEventParameters(
            Event event, org.web3j.protocol.core.methods.response.Log log) {

        List<String> topics = log.getTopics();
        String encodedEventSignature = EventEncoder.encode(event);
        if (!topics.get(0).equals(encodedEventSignature)) {
            return null;
        }

        List<Type> indexedValues = new ArrayList<>();
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                log.getData(), event.getNonIndexedParameters());

        List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
        for (int i = 0; i < indexedParameters.size(); i++) {
            Type value = FunctionReturnDecoder.decodeIndexedValue(
                    topics.get(i + 1), indexedParameters.get(i));
            indexedValues.add(value);
        }
        return new EventValues(indexedValues, nonIndexedValues);
    }
}
