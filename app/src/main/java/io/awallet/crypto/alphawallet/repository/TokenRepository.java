package io.awallet.crypto.alphawallet.repository;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.SubscribeWrapper;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenFactory;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.TokenTicker;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionOperation;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.service.TickerService;
import io.awallet.crypto.alphawallet.service.TokenExplorerClientType;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int16;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import rx.Scheduler;

import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TokenRepository implements TokenRepositoryType {

    private static final String TAG = "TRT";
    private static final long BALANCE_UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS;
    private final TokenExplorerClientType tokenNetworkService;
    private final WalletRepositoryType walletRepository;
    private final TokenLocalSource localSource;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TransactionLocalSource transactionsLocalCache;
    private final TickerService tickerService;
    private Web3j web3j;
    private Web3j web3jFullNode = null;

    public TokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            WalletRepositoryType walletRepository,
            TokenExplorerClientType tokenNetworkService,
            TokenLocalSource localSource,
            TransactionLocalSource transactionsLocalCache,
            TickerService tickerService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.walletRepository = walletRepository;
        this.tokenNetworkService = tokenNetworkService;
        this.localSource = localSource;
        this.transactionsLocalCache = transactionsLocalCache;
        this.tickerService = tickerService;
        this.ethereumNetworkRepository.addOnChangeDefaultNetwork(this::buildWeb3jClient);
        buildWeb3jClient(ethereumNetworkRepository.getDefaultNetwork());
    }

    private void buildWeb3jClient(NetworkInfo defaultNetwork) {
        org.web3j.protocol.http.HttpService publicNodeService = new org.web3j.protocol.http.HttpService(defaultNetwork.rpcServerUrl);
        web3j = Web3jFactory.build(publicNodeService);
        if (defaultNetwork.backupNodeUrl != null)
        {
            org.web3j.protocol.http.HttpService fullNodeService = new org.web3j.protocol.http.HttpService(defaultNetwork.backupNodeUrl);
            web3jFullNode = Web3jFactory.build(fullNodeService);
        }
    }

    @Override
    public Observable<Token[]> fetchActive(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
//        return Single.merge(
//                fetchCachedEnabledTokens(network, wallet), // Immediately show the cache.
//                updateTokens(network, wallet) // Looking for new tokens
//                        .andThen(fetchCachedEnabledTokens(network, wallet))) // and showing the cach
//            .toObservable();
        //This is now handled in transaction view. If we repeat here then we conflict with processing in transaction view
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
    public Observable<Token[]> fetchActiveStoredPlusEth(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokens(network, wallet) // fetch tokens from cache
                .compose(attachEthereumStored(network, wallet)) //add cached eth balance
                .toObservable();
    }

    @Override
    public Observable<Token[]> fetchActiveStored(String walletAddress) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredEnabledTokens(network, wallet) // fetch tokens from cache
                .toObservable();
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

    private Single<Token> attachCachedEth(NetworkInfo network, Wallet wallet) {
        return walletRepository.balanceInWei(wallet)
                .map(balance -> {
                    TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true);
                    return new Token(info, balance, System.currentTimeMillis());
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

//    @Override
//    public Observable<Token> fetchActiveToken(String walletAddress, String address)
//    {
//        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
//        Wallet wallet = new Wallet(walletAddress);
//        return localSource
//                .fetchEnabledToken(network, wallet, address)
//                .flatMap(token -> updateBalance(network, wallet, token))
//                .observeOn(Schedulers.newThread())
//                .toObservable();
//        //return
////                setupTokensFromLocal(address)
////                .flatMap(token -> updateBalance(network, wallet, token))
////                .observeOn(Schedulers.newThread())
////                .toObservable();
//
////        return fetchCachedToken(network, wallet, address)
////                .map(token -> updateBalance(network, wallet, token))
////                .subscribeOn(Schedulers.io());
//    }

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
    public Completable addToken(Wallet wallet, TokenInfo tokenInfo) {
        TokenFactory tf = new TokenFactory();
        Token newToken = tf.createToken(tokenInfo);
        Log.d(TAG, "Create for store2: " + tokenInfo.name);

        return localSource.saveTokens(
                ethereumNetworkRepository.getDefaultNetwork(),
                wallet,
                new Token[] { newToken });
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

    private Single<Token> updateBalance(NetworkInfo network, Wallet wallet, final Token token) {
        return Single.fromCallable(() -> {
            try
            {
                TokenFactory tFactory = new TokenFactory();
                List<BigInteger> balanceArray = null;
                List<Integer> burnArray = null;
                BigDecimal balance = null;
                if (token.tokenInfo.isStormbird)
                {
                    Ticket t = (Ticket) token;
                    balanceArray = getBalanceArray(wallet, t.tokenInfo);
                    burnArray = t.getBurnList();
                }
                else
                {
                    balance = getBalance(wallet, token.tokenInfo);
                }

                Token updated = tFactory.createToken(token.tokenInfo, balance, balanceArray, burnArray, System.currentTimeMillis());
                localSource.updateTokenBalance(network, wallet, updated);
                return updated;
            }
            finally {

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
            if (token.balance == null || token.updateBlancaTime < minUpdateBalanceTime) {
                try {
                    if (token.tokenInfo.isStormbird)
                    {
                        balanceArray = getBalanceArray(wallet, token.tokenInfo);
                        burnArray = ((Ticket)token).getBurnList();
                    }
                    else
                    {
                        balance = getBalance(wallet, token.tokenInfo);
                    }
                    token = tFactory.createToken(token.tokenInfo, balance, balanceArray, burnArray, now);
                    localSource.updateTokenBalance(network, wallet, token);
                } catch (Throwable th) { /* Quietly */ }
            }
            return token;
        });
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

//    @Override
//    public Observable<TokenInfo> update(String contractAddr) {
//        return setupTokensFromLocal(contractAddr).toObservable();
//    }

    @Override
    public rx.Subscription memPoolListener(SubscribeWrapper subscriber)
    {
        if (web3jFullNode != null)
        {
            return web3jFullNode.pendingTransactionObservable()
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
                    TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true);
                    return new Token(info, balance, System.currentTimeMillis());
                })
                .flatMap(token -> ethereumNetworkRepository.getTicker()
                        .map(ticker -> {
                            token.ticker = new TokenTicker("", "", ticker.price_usd, ticker.percentChange24h, null);
                            return token;
                        }).onErrorResumeNext(throwable -> Single.just(token)));
    }

    private BigDecimal getBalance(Wallet wallet, TokenInfo tokenInfo) throws Exception {
        Function function = balanceOf(wallet.address);
        String responseValue = callSmartContractFunction(function, tokenInfo.address, wallet);

        List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return new BigDecimal(((Uint256) response.get(0)).getValue());
        } else {
            return null;
        }
    }

    private List<BigInteger> getBalanceArray(Wallet wallet, TokenInfo tokenInfo) throws Exception {
        List<BigInteger> result = new ArrayList<>();
        if (tokenInfo.isStormbird) //safety check
        {
            boolean newIDFormat = false;
            org.web3j.abi.datatypes.Function function = balanceOfArray2(wallet.address);
            List<Bytes32> indicies = callSmartContractFunctionArray(function, tokenInfo.address, wallet);
            for (Bytes32 val : indicies)
            {
                BigInteger convert;
                if (newIDFormat || testForNewTokenID(val))
                {
                    newIDFormat = true;
                    byte[] temp = new byte[16];
                    System.arraycopy(val.getValue(), 0, temp, 0, 16);
                    convert = Numeric.toBigInt(temp);
                }
                else
                {
                    convert = Numeric.toBigInt(val.getValue());
                }

                //perform some massage on the value
                //result.add(new Bytes32 (Numeric.toBytesPadded(Numeric.toBigInt(val.getValue()), 32)));
                result.add(convert);
            }
        }
        return result;
    }

    private boolean testForNewTokenID(Bytes32 val)
    {
        boolean topBytes = false;
        //are the top 4 bytes set? If so it's Vitalik's strange front-first initialisation format
        for (int i = 0; i < 4; i++)
        {
            byte test = val.getValue()[i];
            if (test > 0)
            {
                topBytes = true;
                break;
            }
        }

        return topBytes;
    }

    private <T> T getContractData(String address, org.web3j.abi.datatypes.Function function) throws Exception
    {
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);


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

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return (String)response.get(0).getValue();
        } else {
            return null;
        }
    }

    private String getSymbol(String address) throws Exception {
        org.web3j.abi.datatypes.Function function = symbolOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, temp);

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
                Collections.singletonList(new TypeReference<DynamicArray<Uint16>>() {}));
    }

    private static org.web3j.abi.datatypes.Function balanceOfArray2(String owner) {
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

        org.web3j.protocol.core.methods.request.Transaction transaction
                = createEthCallTransaction(wallet.address, contractAddress, encodedFunction);
        EthCall response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();

        return response.getValue();
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
        Ticket t = new Ticket(null, "0", "0", 0);
        List ticketIndicies = t.stringDecimalToBigIntegerList(indexListStr);
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
                String name = getContractData(address, stringParam("name"));
                if (name == null)
                {
                    name = getName(address);
                }
                Boolean isStormbird = getContractData(address, boolParam("isStormBirdContract"));
                if (isStormbird == null) isStormbird = false;
                TokenInfo result = new TokenInfo(
                        address,
                        name,
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
                    String name = getContractData(address, stringParam("name"));
                    if (name == null)
                    {
                        name = getName(address);
                    }
                    Boolean isStormbird = getContractData(address, boolParam("isStormBirdContract"));
                    if (isStormbird == null) isStormbird = false;
                    TokenInfo result = new TokenInfo(
                            address,
                            name,
                            getContractData(address, stringParam("symbol")),
                            getDecimals(address),
                            true,
                            isStormbird);

                    if (result.name != null && result.name.length() > 0)
                    {
                        tokenList.add(result);
                    }
                    else
                    {
                        tokenList.add(result);
                    }
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
}
