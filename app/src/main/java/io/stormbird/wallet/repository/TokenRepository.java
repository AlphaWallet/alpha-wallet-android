package io.stormbird.wallet.repository;

import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;
import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.BadContract;
import io.stormbird.token.entity.FunctionDefinition;
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TickerService;
import io.stormbird.wallet.service.TokenExplorerClientType;
import okhttp3.OkHttpClient;
import org.web3j.abi.*;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.WalletUtils;
import org.web3j.ens.EnsResolver;
import org.web3j.ens.contracts.generated.ENS;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.stormbird.wallet.C.BURN_ADDRESS;
import static io.stormbird.wallet.repository.EthereumNetworkRepository.MAINNET_ID;
import static io.stormbird.wallet.repository.EthereumNetworkRepository.POA_ID;
import static io.stormbird.wallet.util.Utils.isAlNum;
import static org.web3j.crypto.WalletUtils.isValidAddress;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TokenRepository implements TokenRepositoryType {

    private static final String TAG = "TRT";
    private final TokenExplorerClientType tokenNetworkService;
    private final WalletRepositoryType walletRepository;
    private final TokenLocalSource localSource;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionLocalSource transactionsLocalCache;
    private final AssetDefinitionService assetDefinitionService;
    private final TickerService tickerService;
    private final TransactionRepositoryType transactionRepository;

    public static final String INVALID_CONTRACT = "<invalid>";

    private static final int NODE_COMMS_ERROR = -1;
    private static final int CONTRACT_BALANCE_NULL = -2;

    private final Map<Integer, Web3j> web3jNodeServers;
    private final OkHttpClient okClient;

    public TokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            WalletRepositoryType walletRepository,
            TokenExplorerClientType tokenNetworkService,
            TokenLocalSource localSource,
            TransactionLocalSource transactionsLocalCache,
            TickerService tickerService,
            AssetDefinitionService assetDefinitionService,
            TransactionRepositoryType transactionRepository) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.walletRepository = walletRepository;
        this.tokenNetworkService = tokenNetworkService;
        this.localSource = localSource;
        this.transactionsLocalCache = transactionsLocalCache;
        this.tickerService = tickerService;
        this.ethereumNetworkRepository.addOnChangeDefaultNetwork(this::buildWeb3jClient);
        this.assetDefinitionService = assetDefinitionService;
        this.transactionRepository = transactionRepository;
        web3jNodeServers = new ConcurrentHashMap<>();
        okClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        buildWeb3jClient(ethereumNetworkRepository.getDefaultNetwork());
    }

    private void buildWeb3jClient(NetworkInfo networkInfo)
    {
        HttpService publicNodeService = new HttpService(networkInfo.rpcServerUrl, okClient, false);
        web3jNodeServers.put(networkInfo.chainId, Web3j.build(publicNodeService));
    }

    private Web3j getService(int chainId)
    {
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId, false);
        if (!web3jNodeServers.containsKey(chainId) && network != null)
        {
            HttpService publicNodeService = new HttpService(network.rpcServerUrl, okClient, false);
            web3jNodeServers.put(chainId, Web3j.build(publicNodeService));
        }

        return web3jNodeServers.get(chainId);
    }

    @Override
    public Observable<Token[]> fetchActiveStoredPlusEth(String walletAddress) {
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokens(wallet) // fetch tokens from cache
                .compose(attachEthereumStored(wallet)) //add cached eth balance
                .compose(attachERC721Tokens(wallet))
                .toObservable();
    }

    @Override
    public Single<BigInteger> fetchLatestBlockNumber(int chainId)
    {
        return Single.fromCallable(() -> {
            try
            {
                EthBlockNumber blk = getService(chainId).ethBlockNumber()
                        .send();
                return blk.getBlockNumber();
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        });
    }

    private Single<Token[]> fetchERC721Tokens(Wallet wallet)
    {
        return localSource.fetchERC721Tokens(wallet);
    }

    @Override
    public Observable<Token[]> fetchActiveStored(String walletAddress) {
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokens(wallet) // fetch tokens from cache
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
                    return result.toArray(new Token[0]);
                });
    }

    private SingleTransformer<Token[], Token[]> attachEthereumStored(Wallet wallet)
    {
        return upstream -> Single.zip(
                upstream, attachCachedEth(wallet),
                (tokens, ethTokens) ->
                {
                    List<Token> result = new ArrayList<>();
                    result.addAll(ethTokens);
                    result.addAll(Arrays.asList(tokens));
                    return result.toArray(new Token[0]);
                });
    }

    private Single<List<Token>> attachCachedEth(Wallet wallet)
    {
        //get stored eth balance
        return Single.fromCallable(() -> {
            Map<Integer, Token> currencies = localSource.getTokenBalances(wallet, wallet.address);
            //always show eth balance, others optional
            boolean hasEth = false;
            NetworkInfo[] allNetworks = ethereumNetworkRepository.getAvailableNetworkList();

            for (NetworkInfo info : allNetworks)
            {
                if (!currencies.containsKey(info.chainId))
                {
                    currencies.put(info.chainId, createCurrencyToken(info, wallet));
                }
            }

            return new ArrayList(currencies.values());
        });
    }

    private Token createCurrencyToken(NetworkInfo network, Wallet wallet)
    {
        TokenInfo tokenInfo = new TokenInfo(wallet.address, network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = BigDecimal.ZERO;
        Token eth = new Token(tokenInfo, balance, System.currentTimeMillis(), network.getShortName(), ContractType.ETHEREUM);
        eth.setTokenWallet(wallet.address);
        eth.setIsEthereum();
        eth.balanceUpdatePressure = 10.0f;
        eth.pendingBalance = balance;
        return eth;
    }

    @Override
    public Observable<Token> fetchActiveSingle(String walletAddress, Token token)
    {
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId, false);
        Wallet wallet = new Wallet(walletAddress);
        return Single.merge(
                fetchCachedToken(network, wallet, token.getAddress()),
                updateBalance(network, wallet, token)) // Looking for new tokens
                .toObservable();
    }

    @Override
    public Observable<Token> fetchCachedSingleToken(NetworkInfo network, String walletAddress, String tokenAddress)
    {
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
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId, false);
        Wallet wallet = new Wallet(walletAddress);
        return updateBalance(network, wallet, token)
                .observeOn(Schedulers.newThread())
                .toObservable();
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
    public Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo, ContractType interfaceSpec)
    {
        TokenFactory tf = new TokenFactory();
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId, false);

        //check balance before we store it
        List<BigInteger> balanceArray = null;
        BigDecimal balance = BigDecimal.ZERO;
        switch (interfaceSpec)
        {
            case ERC875:
            case ERC875LEGACY:
                balanceArray = wrappedCheckBalanceArray(wallet, tokenInfo, null);
                break;
            case ETHEREUM:
            case ERC20:
                balance = wrappedCheckUintBalance(wallet, tokenInfo, null);
                break;
            default:
                break;
        }
        Token newToken = tf.createToken(tokenInfo, balance, balanceArray, System.currentTimeMillis(), interfaceSpec, network.getShortName(), 0);
        newToken.setTokenWallet(wallet.address);
        Log.d(TAG, "Create for store: " + tokenInfo.name);

        if (newToken.hasPositiveBalance()) newToken.walletUIUpdateRequired = true;

        return localSource.saveToken(
                wallet,
                newToken);
    }

    //TODO: This should be called on a per-token basis. User will usually only have one token, so currently it's ok
    @Override
    public Single<Token> callTokenFunctions(Token token, AssetDefinitionService service)
    {
        TokenDefinition definition = service.getAssetDefinition(token.getAddress());
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId, false);

        if (definition == null || definition.functions.size() == 0 || !token.requiresAuxRefresh()) return Single.fromCallable(() -> token); //quick return
        else return Single.fromCallable(() -> {
            token.auxDataRefreshed();
            //need to call the token functions now
            for (String keyName : definition.functions.keySet())
            {
                FunctionDefinition fd = definition.functions.get(keyName);
                //currently all function calls from XML have no params
                String result = null;
                switch (fd.syntax)
                {
                    case Boolean:
                        result = callBoolFunction(fd.method, token.getAddress(), network);
                        break;
                    case IA5String:
                        if (!token.getTokenID(0).equals(BigInteger.valueOf(-1)))
                        {
                            result = callStringFunction(fd.method, token.getAddress(), network, token.getTokenID(0));
                        }
                        break;
                    default:
                        break;
                }

                token.addAuxDataResult(keyName, result);
            }
            return token;
        });
    }

    private String callStringFunction(String method, String address, NetworkInfo network, BigInteger tokenId)
    {
        String result;
        try
        {
            result = getContractData(network, address, stringParam(method, tokenId), "");
        }
        catch (Exception e)
        {
            result = null;
        }

        return result;
    }

    private String callBoolFunction(String method, String address, NetworkInfo network)
    {
        String result;
        try
        {
            Boolean res = getContractData(network, address, boolParam(method), Boolean.TRUE);
            result = res ? "true" : "false";
        }
        catch (Exception e)
        {
            result = "false";
        }

        return result;
    }

    @Override
    public Single<Token[]> addERC721(Wallet wallet, Token[] tokens)
    {
        return localSource.saveERC721Tokens(
                wallet,
                tokens);
    }

    @Override
    public Single<String> resolveENS(int chainId, String address)
    {
        return Single.fromCallable(() -> {
            int useChainId = chainId;
            if (!EthereumNetworkRepository.hasRealValue(useChainId)) useChainId = MAINNET_ID;
            Web3j service = getService(useChainId); //resolve ENS on mainnet unless this network has value
            EnsResolver ensResolver = new EnsResolver(service);
            String resolvedAddress = ensResolver.resolve(address);
            if (WalletUtils.isValidAddress(resolvedAddress))
            {
                return resolvedAddress;
            }
            else
            {
                return BURN_ADDRESS;
            }
        });
    }

    @Override
    public Completable setEnable(Wallet wallet, Token token, boolean isEnabled) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        return Completable.fromAction(() -> localSource.setEnable(network, wallet, token, isEnabled));
    }

    @Override
    public Observable<TokenInfo> update(String contractAddr, int chainId) {
        return setupTokensFromLocal(contractAddr, chainId).toObservable();
    }

    @Override
    public Disposable terminateToken(Token token, Wallet wallet, NetworkInfo network)
    {
        return localSource.setTokenTerminated(token, network, wallet);
    }

    @Override
    public Single<TokenInfo[]> update(String[] address, NetworkInfo network)
    {
        return setupTokensFromLocal(address, network);
    }

//    private Single<Token[]> fetchFromNetworkSource(@NonNull NetworkInfo network, @NonNull Wallet wallet) {
//        return Single.fromCallable(() -> {
//            try {
//                return network.isMainNetwork
//                        ? tokenNetworkService.fetch(wallet.address).blockingFirst()
//                        : new TokenInfo[0];
//            } catch (Throwable th) {
//                // Ignore all errors, it's not important source.
//                return new TokenInfo[0];
//            }
//        })
//        .map(this::mapToTokens);
//    }

//    private Single<Token[]> extractFromTransactions(NetworkInfo network, Wallet wallet) {
//        return transactionsLocalCache.fetchTransaction(network, wallet)
//                .flatMap(transactions -> {
//                    List<Token> result = new ArrayList<>();
//                    for (Transaction transaction : transactions) {
//                        if (transaction.operations == null || transaction.operations.length == 0) {
//                            continue;
//                        }
//                        TransactionOperation operation = transaction.operations[0];
//                        result.add(new Token(new TokenInfo(
//                                operation.contract.address,
//                                operation.contract.name,
//                                operation.contract.symbol,
//                                operation.contract.decimals,
//                                true, network.chainId), null, 0
//                        ,network.getShortName()));
//                    }
//                    return Single.just(result.toArray(new Token[result.size()]));
//                });
//    }
//
//    private Completable updateTokens(NetworkInfo network, Wallet wallet) {
//        return Single.zip(
//                fetchFromNetworkSource(network, wallet),
//                extractFromTransactions(network, wallet),
//                localSource.fetchAllTokens(network, wallet),
//                (fromNetTokens, fromTrxTokens, cachedTokens) -> {
//                    final Set<String> oldTokensIndex = new HashSet<>();
//                    final List<Token> zip = new ArrayList<>();
//                    zip.addAll(Arrays.asList(fromNetTokens));
//                    zip.addAll(Arrays.asList(fromTrxTokens));
//                    final List<Token> newTokens = new ArrayList<>();
//                    for (Token cachedToken : cachedTokens) {
//                        oldTokensIndex.add(cachedToken.tokenInfo.address);
//                    }
//                    for (int i = zip.size() - 1; i > -1; i--) {
//                        if (!oldTokensIndex.contains(zip.get(i).tokenInfo.address)) {
//                            newTokens.add(zip.get(i));
//                        }
//                    }
//                    return newTokens.toArray(new Token[newTokens.size()]);
//                })
//                .flatMapCompletable(tokens -> localSource.saveTokens(network, wallet, tokens));
//    }

    /**
     * Obtain live balance of token from Ethereum blockchain and cache into Realm
     *
     * @param network
     * @param wallet
     * @param token
     * @return
     */
    private Single<Token> updateBalance(NetworkInfo network, Wallet wallet, final Token token) {
        if (token == null) return Single.fromCallable(() -> null);
        else if (token.isEthereum())
        {
            return attachEth(network, wallet, token);
        }
        else
        return Single.fromCallable(() -> {
            TokenFactory tFactory = new TokenFactory();
            try
            {
                List<BigInteger> balanceArray = null;
                BigDecimal balance = BigDecimal.ZERO;
                TokenInfo tInfo = token.tokenInfo;
                ContractType interfaceSpec = token.getInterfaceSpec();
                boolean balanceChanged = false;
                switch (interfaceSpec)
                {
                    case NOT_SET:
                        if (token.tokenInfo.name != null && token.tokenInfo.name.length() > 0)
                        {
                            Log.d(TAG, "NOT SET: " + token.getFullName());
                        }
                        break;
                    case ERC875:
                    case ERC875LEGACY:
                        balanceArray = wrappedCheckBalanceArray(wallet, tInfo, token);
                        balanceChanged = token.checkBalanceChange(balanceArray);
                        break;
                    case ERC721:
                        break;
                    case ETHEREUM:
                    case ERC20:
                        balance = wrappedCheckUintBalance(wallet, token.tokenInfo, token);
                        if (balance.compareTo(BigDecimal.ZERO) < 0) balance = token.balance;
                        balanceChanged = token.checkBalanceChange(balance);
                        break;
                    case OTHER:
                        //TODO: periodic re-check of contract, may have sufficient data in future to determine token type
                        //balance = wrappedCheckUintBalance(wallet, token.tokenInfo, token);
                        break;
                    default:
                        break;
                }

                //check if we need an update
                if (balanceChanged)
                {
                    Log.d(TAG, "Token balance changed! " + tInfo.name);
                    Token updated = tFactory.createToken(tInfo, balance, balanceArray, System.currentTimeMillis(), interfaceSpec, network.getShortName(), token.lastBlockCheck);
                    updated.patchAuxData(token); //perform any updates we need here
                    localSource.updateTokenBalance(network, wallet, updated);
                    updated.setTokenWallet(wallet.address);
                    updated.transferPreviousData(token);
                    updated.balanceChanged = true;
                    updated.pendingBalance = balance;
                    return updated;
                }
                else
                {
                    return token;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return token;
            }
        });
    }

    /**
     * Checks the balance of a token returning Uint256 value, eg Ethereum, ERC20
     * If there was a network error the balance is taken from the previously recorded value
     * @param wallet
     * @param tokenInfo
     * @param token
     * @return
     */
    private BigDecimal wrappedCheckUintBalance(Wallet wallet, TokenInfo tokenInfo, Token token)
    {
        BigDecimal balance = BigDecimal.ZERO;
        try
        {
            balance = getBalance(wallet, tokenInfo);
            if (token != null && balance.compareTo(BigDecimal.valueOf(NODE_COMMS_ERROR)) == 0)
            {
                balance = token.balance;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return balance;
    }

    /**
     * Checks the balance array for an ERC875 token.
     * The balance function returns a value of NODE_COMMS_ERROR in the first entry if there was a comms error.
     * In the event of a comms error, just use the previously obtained balance of the token
     * @param wallet
     * @param tInfo
     * @param token
     * @return
     */
    private List<BigInteger> wrappedCheckBalanceArray(Wallet wallet, TokenInfo tInfo, Token token)
    {
        List<BigInteger> balance = getBalanceArray(wallet, tInfo);

        if (balance.size() > 0)
        {
            BigInteger firstVal = balance.get(0);
            if (token != null && firstVal.compareTo(BigInteger.valueOf(NODE_COMMS_ERROR)) == 0)
            {
                //comms error, use previous token balance
                balance = token.getArrayBalance();
            }
            else if (firstVal.compareTo(BigInteger.valueOf(CONTRACT_BALANCE_NULL)) == 0)
            {
                //token could have been terminated
                balance.clear();
            }
        }

        return balance;
    }

    @Override
    public Disposable memPoolListener(int chainId, SubscribeWrapper subscriber)
    {
        return getService(chainId).pendingTransactionFlowable().subscribe(subscriber::scanReturn);
    }

    private Single<Token[]> fetchStoredEnabledTokens(Wallet wallet) {
        return localSource
                .fetchEnabledTokensWithBalance(wallet);
    }

    private Single<Token> fetchCachedToken(NetworkInfo network, Wallet wallet, String address)
    {
        return localSource
                .fetchEnabledToken(network, wallet, address);
    }

    private Single<Token> attachEth(NetworkInfo network, Wallet wallet, Token oldToken) {
        final boolean pending = !oldToken.balance.equals(BigDecimal.ZERO) && oldToken.balance.equals(oldToken.pendingBalance);

        return getEthBalanceInternal(network, wallet, pending)
                .map(balance -> {
                    BigDecimal oldBalance = oldToken.balance;
                    if (balance.equals(BigDecimal.valueOf(-1))) return oldToken;

                    if (pending && !balance.equals(oldToken.pendingBalance))
                    {
                        Log.d(TAG, "ETH: " + balance.toPlainString() + " OLD: " + oldBalance.toPlainString());
                        oldToken.pendingBalance = balance;
                        return oldToken;
                    }
                    else if (!balance.equals(oldBalance))
                    {
                        Log.d(TAG, "Tx Update requested for: " + oldToken.getFullName());
                        TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true,
                                                       network.chainId);
                        Token eth = new Token(info, balance, System.currentTimeMillis(), network.getShortName(), ContractType.ETHEREUM);
                        eth.setTokenWallet(wallet.address);
                        //store token and balance
                        localSource.updateTokenBalance(network, wallet, eth);
                        eth.balanceChanged = true;
                        eth.transferPreviousData(oldToken);
                        eth.pendingBalance = balance;
                        return eth;
                    }
                    else
                    {
                        return oldToken;
                    }
                })
                .flatMap(token -> ethereumNetworkRepository.getTicker(network.chainId)
                        .map(ticker -> {
                            token.ticker = new TokenTicker(String.valueOf(network.chainId), wallet.address, ticker.price_usd, ticker.percentChange24h, null);
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
        Token currency = createCurrencyToken(network, wallet);
        currency.pendingBalance = BigDecimal.ONE;
        return attachEth(network, wallet, currency);
    }

    @Override
    public Single<Ticker> getEthTicker(int chainId)
    {
        return ethereumNetworkRepository.getTicker(chainId);
    }

    private BigDecimal getBalance(Wallet wallet, TokenInfo tokenInfo) throws Exception {
        Function function = balanceOf(wallet.address);
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId, false);
        String responseValue = callSmartContractFunction(function, tokenInfo.address, network, wallet);

        if (responseValue == null) return BigDecimal.valueOf(-1); //early return for network error

        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return BigDecimal.ZERO;
        }
    }

    private Single<BigDecimal> getEthBalanceInternal(NetworkInfo network, Wallet wallet, boolean pending)
    {
        return Single.fromCallable(() -> {
            try {
                if (pending)
                {
                    return new BigDecimal(getService(network.chainId).ethGetBalance(wallet.address, DefaultBlockParameterName.PENDING)
                                                  .send()
                                                  .getBalance());
                }
                else
                {
                    return new BigDecimal(getService(network.chainId).ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST)
                                                  .send()
                                                  .getBalance());
                }
            }
            catch (IOException e)
            {
                return BigDecimal.valueOf(-1);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return BigDecimal.valueOf(-1);
            }
        }).subscribeOn(Schedulers.io());
    }

    private List<BigInteger> getBalanceArray(Wallet wallet, TokenInfo tokenInfo) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            org.web3j.abi.datatypes.Function function = balanceOfArray(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId, false);
            List<Type> indices = callSmartContractFunctionArray(function, tokenInfo.address, network, wallet);
            if (indices != null)
            {
                result.clear();
                for (Type val : indices)
                {
                    result.add((BigInteger)val.getValue());
                }
            }
        }
        catch (StringIndexOutOfBoundsException e)
        {
            //contract call error
            result.clear();
            result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        }
        return result;
    }

    public Observable<TransferFromEventResponse> burnListenerObservable(String contractAddr)
    {
        return Observable.fromCallable(() -> {
            TransferFromEventResponse event = new TransferFromEventResponse();
            event._from = "";
            event._to = "";
            event._indices = null;
            return event;
        });
    }

    private <T> T getContractData(NetworkInfo network, String address, org.web3j.abi.datatypes.Function function, T type) throws Exception
    {
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, network, temp);

        if (responseValue == null)
        {
            if (type instanceof Boolean)
            {
                return (T)Boolean.FALSE;
            }
            else
            {
                return null;
            }
        }

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1)
        {
            if (type instanceof String)
            {
                String value = (String)response.get(0).getValue();
                if (value.length() == 0 && responseValue.length() > 2)
                {
                    value = checkBytesString(responseValue);
                    if (!isAlNum(value)) value = "";
                    return (T)value;
                }
            }
            return (T) response.get(0).getValue();
        }
        else
        {
            return null;
        }
    }

    private String checkBytesString(String responseValue) throws Exception
    {
        String name = "";
        if (responseValue.length() > 0)
        {
            //try raw bytes
            byte[] data = Numeric.hexStringToByteArray(responseValue);
            //check leading bytes for non-zero
            if (data[0] != 0)
            {
                //truncate zeros
                int index = data.length - 1;
                while (data[index] == 0 && index > 0)
                    index--;
                if (index != (data.length - 1))
                {
                    data = Arrays.copyOfRange(data, 0, index + 1);
                }
                name = new String(data, "UTF-8");
            }
        }

        return name;
    }

    private String getName(String address, NetworkInfo network) throws Exception {
        org.web3j.abi.datatypes.Function function = nameOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, network ,temp);

        if (responseValue == null) return null;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            String name = (String)response.get(0).getValue();
            if (responseValue.length() > 2 && name.length() == 0)
            {
                name = checkBytesString(responseValue);
            }
            if (assetDefinitionService.getChainId(address) > 0)
            {
                //does name already contain the token type
                String tokenTypeName = assetDefinitionService.getAssetDefinition(address).getTokenName();
                if (name != null && !name.contains(tokenTypeName)) name = name + " " + tokenTypeName;
            }
            return name;
        } else {
            return null;
        }
    }

    private int getDecimals(String address, NetworkInfo network) throws Exception {
        org.web3j.abi.datatypes.Function function = decimalsOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, network, temp);
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
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
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

    private static org.web3j.abi.datatypes.Function stringParam(String param, BigInteger value) {
        return new Function(param,
                            Arrays.asList(new Uint256(value)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
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

    private static org.web3j.abi.datatypes.Function addrParam(String param) {
        return new Function(param,
                            Arrays.<Type>asList(),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    }

    private org.web3j.abi.datatypes.Function addressFunction(String method, byte[] resultHash)
    {
        return new org.web3j.abi.datatypes.Function(
                method,
                Collections.singletonList(new org.web3j.abi.datatypes.generated.Bytes32(resultHash)),
                Collections.singletonList(new TypeReference<Address>() {}));
    }

    private List callSmartContractFunctionArray(
            org.web3j.abi.datatypes.Function function, String contractAddress, NetworkInfo network, Wallet wallet)
    {
        try
        {
            String encodedFunction = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.response.EthCall ethCall = getService(network.chainId).ethCall(
                    org.web3j.protocol.core.methods.request.Transaction
                            .createEthCallTransaction(wallet.address, contractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST).send();

            String value = ethCall.getValue();
            List<Type> values = FunctionReturnDecoder.decode(value, function.getOutputParameters());
            Object o;
            if (values.isEmpty())
            {
                values = new ArrayList<Type>();
                values.add((Type)new Int256(CONTRACT_BALANCE_NULL));
                o = (List)values;
            }
            else
            {
                Type T = values.get(0);
                o = T.getValue();
            }
            return (List) o;
        }
        catch (IOException e) //this call is expected to be interrupted when user switches network or wallet
        {
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private String callSmartContractFunction(
            Function function, String contractAddress, NetworkInfo network, Wallet wallet) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(wallet.address, contractAddress, encodedFunction);
            EthCall response = getService(network.chainId).ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (IOException e) //this call is expected to be interrupted when user switches network or wallet
        {
            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Call smart contract function on custom network contract. This would be used for things like ENS lookup
     * Currently because it's tied to a mainnet contract address there's no circumstance it would work
     * outside of mainnet. Users may be confused if their namespace doesn't work, even if they're currently
     * using testnet.
     *
     * @param function
     * @param contractAddress
     * @param wallet
     * @return
     */
    private String callCustomNetSmartContractFunction(
            Function function, String contractAddress, Wallet wallet, int chainId)  {
        String encodedFunction = FunctionEncoder.encode(function);

        try
        {
            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(wallet.address, contractAddress, encodedFunction);
            EthCall response = getService(chainId).ethCall(transaction, DefaultBlockParameterName.LATEST).send();

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

    public static byte[] createTicketTransferData(String to, String indexListStr, Token token) {
        List ticketIndicies = Observable.fromArray(indexListStr.split(","))
                .map(s -> new BigInteger(s.trim())).toList().blockingGet();
        Function function = ((Ticket)token).getTransferFunction(to, ticketIndicies);

        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createERC721TransferFunction(String to, Token token, String tokenId)
    {
        Function function = token.getTransferFunction(to, tokenId);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createTrade(Token token, BigInteger expiry, List<BigInteger> ticketIndices, int v, byte[] r, byte[] s)
    {
        Function function = ((Ticket)token).getTradeFunction(expiry, ticketIndices, v, r, s);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createSpawnPassTo(Token token, BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s, String recipient)
    {
        Function function = ((Ticket)token).getSpawnPassToFunction(expiry, tokenIds, v, r, s, recipient);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createDropCurrency(MagicLinkData order, int v, byte[] r, byte[] s, String recipient)
    {
        Function function = new Function(
                "dropCurrency",
                Arrays.asList(new org.web3j.abi.datatypes.generated.Uint32(order.nonce),
                              new org.web3j.abi.datatypes.generated.Uint32(order.amount),
                              new org.web3j.abi.datatypes.generated.Uint32(order.expiry),
                              new org.web3j.abi.datatypes.generated.Uint8(v),
                              new org.web3j.abi.datatypes.generated.Bytes32(r),
                              new org.web3j.abi.datatypes.generated.Bytes32(s),
                              new org.web3j.abi.datatypes.Address(recipient)),
                Collections.emptyList());

        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

//    private Token[] mapToTokens(TokenInfo[] items) {
//        int len = items.length;
//        Token[] tokens = new Token[len];
//        for (int i = 0; i < len; i++)
//        {
//            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(items[i].chainId);
//            tokens[i] = new Token(items[i], null, 0, network.getShortName(), 0);
//        }
//        return tokens;
//    }

    @Override
    public Single<ContractResult> getTokenResponse(String address, int chainId, String method)
    {
        return Single.fromCallable(() -> {
            ContractResult contractResult = new ContractResult(INVALID_CONTRACT, chainId);
            org.web3j.abi.datatypes.Function function = new Function(method,
                                                                     Arrays.<Type>asList(),
                                                                     Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));

            Wallet temp = new Wallet(null);
            String responseValue = callCustomNetSmartContractFunction(function, address, temp, chainId);
            if (responseValue == null) return contractResult;

            List<Type> response = FunctionReturnDecoder.decode(
                    responseValue, function.getOutputParameters());
            if (response.size() == 1)
            {
                contractResult.name = (String) response.get(0).getValue();
                return contractResult;
            }
            else
            {
                return contractResult;
            }
        });
    }

    private Single<TokenInfo> setupTokensFromLocal(String address, int chainId)
    {
        return Single.fromCallable(() -> {
            try
            {
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId, false);
                return new TokenInfo(
                        address,
                        getName(address, network),
                        getContractData(network, address, stringParam("symbol"), ""),
                        getDecimals(address, network),
                        true, chainId);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        });
    }

    private Single<TokenInfo[]> setupTokensFromLocal(String[] addresses, NetworkInfo network)
    {
        return Single.fromCallable(() -> {
            List<TokenInfo> tokenList = new ArrayList<>();
            try
            {
                for (String address : addresses)
                {
                    String name = getName(address, network);
                    TokenInfo result = new TokenInfo(
                            address,
                            name,
                            getContractData(network, address, stringParam("symbol"), ""),
                            getDecimals(address, network),
                            true, ethereumNetworkRepository.getDefaultNetwork().chainId);

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

    @Override
    public Single<String> resolveProxyAddress(TokenInfo tokenInfo)
    {
        return Single.fromCallable(() -> {
            String contractAddress = tokenInfo.address;
            try
            {
                NetworkInfo networkInfo = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId, false);
                String received = getContractData(networkInfo, tokenInfo.address, addrParam("implementation"), "");
                if (received != null && isValidAddress(received))
                {
                    contractAddress = received;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return contractAddress;
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

    @Override
    public Disposable updateBlockRead(Token token, Wallet wallet)
    {
        return localSource.storeBlockRead(token, wallet);
    }
}
