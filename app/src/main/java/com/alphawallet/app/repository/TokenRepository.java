package com.alphawallet.app.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SubscribeWrapper;
import com.alphawallet.app.entity.TransferFromEventResponse;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.MagicLinkData;

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
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.EthereumNetworkRepository.MAINNET_ID;
import static org.web3j.crypto.WalletUtils.isValidAddress;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TokenRepository implements TokenRepositoryType {

    private static final String TAG = "TRT";
    private final TokenLocalSource localSource;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final GasService gasService;

    public static final String INVALID_CONTRACT = "<invalid>";

    public static final BigInteger INTERFACE_CRYPTOKITTIES = new BigInteger ("9a20483d", 16);
    public static final BigInteger INTERFACE_OFFICIAL_ERC721 = new BigInteger ("80ac58cd", 16);
    public static final BigInteger INTERFACE_OLD_ERC721 = new BigInteger ("6466353c", 16);
    public static final BigInteger INTERFACE_BALANCES_721_TICKET = new BigInteger ("c84aae17", 16);


    private static final int NODE_COMMS_ERROR = -1;
    private static final int CONTRACT_BALANCE_NULL = -2;

    private final Map<Integer, Web3j> web3jNodeServers;
    private final OkHttpClient okClient;

    public TokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenLocalSource localSource,
            GasService gasService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.localSource = localSource;
        this.ethereumNetworkRepository.addOnChangeDefaultNetwork(this::buildWeb3jClient);
        this.gasService = gasService;

        web3jNodeServers = new ConcurrentHashMap<>();
        okClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    private void buildWeb3jClient(NetworkInfo networkInfo)
    {
        HttpService publicNodeService = new HttpService(networkInfo.rpcServerUrl, okClient, false);
        EthereumNetworkRepository.addRequiredCredentials(networkInfo.chainId, publicNodeService);
        web3jNodeServers.put(networkInfo.chainId, Web3j.build(publicNodeService));
    }

    private Web3j getService(int chainId)
    {
        if (!web3jNodeServers.containsKey(chainId))
        {
            buildWeb3jClient(ethereumNetworkRepository.getNetworkByChain(chainId));
        }
        return web3jNodeServers.get(chainId);
    }

    @Override
    public Observable<Token[]> fetchActiveStoredPlusEth(String walletAddress) {
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredTokens(wallet) // fetch tokens from cache
                .compose(attachDefaultTokens(wallet))
                .compose(attachEthereumStored(wallet)) //add cached eth balance
                .toObservable();
    }

    private SingleTransformer<Token[], Token[]> attachDefaultTokens(Wallet wallet)
    {
        return upstream -> Single.zip(
                upstream, ethereumNetworkRepository.getBlankOverrideTokens(wallet),
                (tokens, defaultTokens) ->
                {
                    List<Token> result = mergeTokens(tokens, defaultTokens);
                    return result.toArray(new Token[0]);
                });
    }

    private List<Token> mergeTokens(Token[] tokens, Token[] defaultTokens)
    {
        Map<Integer, Map<String, Token>> tokenMergeMap = new HashMap<>();
        for (Token t : defaultTokens)
        {
            if (!tokenMergeMap.containsKey(t.tokenInfo.chainId)) tokenMergeMap.put(t.tokenInfo.chainId, new HashMap<>());
            tokenMergeMap.get(t.tokenInfo.chainId).put(t.tokenInfo.address, t);
        }

        //replace with cached tokens
        for (Token t : tokens)
        {
            if (!tokenMergeMap.containsKey(t.tokenInfo.chainId)) tokenMergeMap.put(t.tokenInfo.chainId, new HashMap<>());
            tokenMergeMap.get(t.tokenInfo.chainId).put(t.tokenInfo.address, t);
        }

        List<Token> tokenList = new ArrayList<>();

        for (int i : tokenMergeMap.keySet())
        {
            tokenList.addAll(tokenMergeMap.get(i).values());
        }

        return tokenList;
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

    @Override
    public Observable<Token[]> fetchStored(String walletAddress) {
        Wallet wallet = new Wallet(walletAddress);
        return fetchStoredTokens(wallet) // fetch tokens from cache
                .toObservable();
    }

    private SingleTransformer<Token[], Token[]> attachEthereumStored(Wallet wallet)
    {
        return upstream -> Single.zip(
                upstream, attachCachedEth(wallet),
                (tokens, ethTokens) ->
                {
                    List<Token> result = new ArrayList<>();
                    result.addAll(ethTokens);
                    for (Token t : tokens) if (!t.isEthereum()) result.add(t);
                    return result.toArray(new Token[0]);
                });
    }

    private Single<List<Token>> attachCachedEth(Wallet wallet)
    {
        //get stored eth balance
        return Single.fromCallable(() -> {
            Map<Integer, Token> currencies = localSource.getTokenBalances(wallet, wallet.address);
            //always show eth balance, others optional
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
        Token eth = new Token(tokenInfo, balance, 0, network.getShortName(), ContractType.ETHEREUM); //create with zero time index to ensure it's updated immediately
        eth.setTokenWallet(wallet.address);
        eth.setIsEthereum();
        eth.pendingBalance = balance;
        return eth;
    }

    @Override
    public Observable<Token> fetchActiveSingle(String walletAddress, Token token)
    {
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId);
        Wallet wallet = new Wallet(walletAddress);
        return Single.merge(
                fetchCachedToken(network.chainId, wallet, token.getAddress()),
                updateBalance(network, wallet, token)) // Looking for new tokens
                .toObservable();
    }

    @Override
    public Observable<Token> fetchCachedSingleToken(int chainId, String walletAddress, String tokenAddress)
    {
        Wallet wallet = new Wallet(walletAddress);
        return fetchCachedToken(chainId, wallet, tokenAddress)
                .toObservable();
    }

    @Override
    public Token fetchToken(int chainId, String walletAddress, String address)
    {
        Wallet wallet = new Wallet(walletAddress);
        return localSource.fetchToken(chainId, wallet, address);
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
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId);
        Wallet wallet = new Wallet(walletAddress);
        return updateBalance(network, wallet, token)
                .observeOn(Schedulers.newThread())
                .toObservable();
    }

    @Override
    public Single<Token> addToken(Wallet wallet, TokenInfo tokenInfo, ContractType contractType)
    {
        return Single.fromCallable(() -> {
            TokenFactory tf      = new TokenFactory();
            NetworkInfo  network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);

            //check balance before we store it
            List<BigInteger> balanceArray = null;
            BigDecimal       balance      = BigDecimal.ZERO;
            switch (contractType)
            {
                case ERC875:
                case ERC875_LEGACY:
                    balanceArray = checkERC875BalanceArray(wallet, tokenInfo, null);
                    break;
                case ERC721_LEGACY:
                case ERC721:
                    break;
                case ERC721_TICKET:
                    balanceArray = checkERC721TicketBalanceArray(wallet, tokenInfo, null);
                    break;
                case ETHEREUM:
                case ERC20:
                    balance = wrappedCheckUint256Balance(wallet, tokenInfo, null); //this checks Uint256 balance
                    if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
                    break;
                default:
                    break;
            }
            Token newToken = tf.createToken(tokenInfo, balance, balanceArray, System.currentTimeMillis(), contractType, network.getShortName(), 0);
            newToken.setTokenWallet(wallet.address);
            newToken.ticker = ethereumNetworkRepository.getTokenTicker(newToken);
            return newToken;
        }).flatMap(nToken -> localSource.saveToken(wallet, nToken));
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
    public Single<Token[]> storeTokens(Wallet wallet, Token[] tokens)
    {
        return localSource.saveTokens(
                wallet,
                tokens);
    }

    @Override
    public Single<Token[]> addERC20(Wallet wallet, Token[] tokens)
    {
        return localSource.saveERC20Tokens(
                wallet,
                tokens);
    }

    @Override
    public Token updateTokenType(Token token, Wallet wallet, ContractType type)
    {
        return localSource.updateTokenType(token, wallet, type);
    }

    @Override
    public Single<Token[]> storeTickers(Wallet wallet, Token[] tokens)
    {
        return localSource.saveTickers(wallet, tokens);
    }

    @Override
    public Single<String> resolveENS(int chainId, String address)
    {
        return Single.fromCallable(() -> {
            String resolvedAddress = resolveAddress(chainId, address);
            if (!WalletUtils.isValidAddress(resolvedAddress))
            {
                resolvedAddress = resolveAddress(MAINNET_ID, address); //try main net
            }

            if (WalletUtils.isValidAddress(resolvedAddress))
            {
                return resolvedAddress;
            }
            else
            {
                return C.BURN_ADDRESS;
            }
        });
    }

    private String resolveAddress(int chainId, String address)
    {
        int useChainId = chainId;
        if (!EthereumNetworkRepository.hasRealValue(useChainId)) useChainId = MAINNET_ID;
        Web3j service = getService(useChainId); //resolve ENS on mainnet unless this network has value
        AWEnsResolver ensResolver = new AWEnsResolver(service, gasService);
        String resolvedAddress = "";
        try
        {
            resolvedAddress = ensResolver.resolve(address);
        }
        catch (Exception e)
        {
            return "--";
        }

        return resolvedAddress;
    }

    @Override
    public Completable setEnable(Wallet wallet, Token token, boolean isEnabled) {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        localSource.setEnable(network, wallet, token, isEnabled);
        return Completable.fromAction(() -> {});
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
                    case ERC875_LEGACY:
                        balanceArray = checkERC875BalanceArray(wallet, tInfo, token);
                        balanceChanged = token.checkBalanceChange(balanceArray);
                        break;
                    case ERC721_LEGACY:
                    case ERC721:
                        break;
                    case ERC721_TICKET:
                        balanceArray = checkERC721TicketBalanceArray(wallet, tInfo, token);
                        balanceChanged = token.checkBalanceChange(balanceArray);
                        break;
                    case ETHEREUM:
                    case ERC20:
                        balance = wrappedCheckUint256Balance(wallet, token.tokenInfo, token);
                        balanceChanged = token.checkBalanceChange(balance);
                        break;
                    case OTHER:
                        //TODO: periodic re-check of contract, may have sufficient data in future to determine token type
                        break;
                    default:
                        break;
                }

                //check if we need an update
                if (balanceChanged)
                {
                    Log.d(TAG, "Token balance changed! " + tInfo.name);
                    Token updated = tFactory.createToken(tInfo, balance, balanceArray, System.currentTimeMillis(), interfaceSpec, network.getShortName(), token.lastBlockCheck);
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
        })
        .flatMap(ethereumNetworkRepository::attachTokenTicker)
        .flatMap(ttoken -> localSource.saveTicker(wallet, ttoken));
    }

    /**
     * Checks the balance of a token returning Uint256 value, eg ERC20
     * If there was a network error the balance is taken from the previously recorded value
     * @param wallet
     * @param tokenInfo
     * @param token
     * @return
     */
    private BigDecimal wrappedCheckUint256Balance(@NonNull Wallet wallet, @NonNull TokenInfo tokenInfo, @Nullable Token token)
    {
        BigDecimal balance = BigDecimal.ZERO;
        try
        {
            Function function = balanceOf(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
            String responseValue = callSmartContractFunction(function, tokenInfo.address, network, wallet);

            if (token != null && responseValue == null)
            {
                balance = token.balance;
            }
            else
            {
                List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
                if (response.size() > 0) balance = new BigDecimal(((Uint256) response.get(0)).getValue());

                //only perform checking if token is non-null
                if (token != null && tokenInfo.decimals == 18 && balance.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(BigDecimal.valueOf(10)) < 0)
                {
                    //suspicious balance - check for ERC721 ticket
                    List<BigInteger> testBalance = getBalanceArray721Ticket(wallet, tokenInfo);
                    if (testBalance != null && testBalance.size() > 0)
                    {
                        addToken(wallet, tokenInfo, ContractType.ERC721_TICKET)
                                .subscribe(this::updateInService).isDisposed();
                        balance = token.balance;
                    }
                }
                else if (token != null && balance.equals(BigDecimal.valueOf(32)) && responseValue.length() > 66)
                {
                    //this is a token returning an array balance. Test the interface and update
                    determineCommonType(tokenInfo)
                            .flatMap(tType -> addToken(wallet, tokenInfo, tType)) //changes token type
                            .subscribe(this::updateInService).isDisposed();
                    balance = token.balance;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return balance;
    }

    private void updateInService(Token t)
    {
        t.walletUIUpdateRequired = true;
        TokensService.setInterfaceSpec(t.tokenInfo.chainId, t.getAddress(), t.getInterfaceSpec());
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
    private List<BigInteger> checkERC875BalanceArray(Wallet wallet, TokenInfo tInfo, Token token)
    {
        List<BigInteger> balance = getBalanceArray875(wallet, tInfo);
        return checkBalanceArrayValidity(balance, token);
    }

    // Only works with 721 tickets that have a special getBalances function which returns an array of uint256
    private List<BigInteger> checkERC721TicketBalanceArray(Wallet wallet, TokenInfo tInfo, Token token)
    {
        List<BigInteger> balance = getBalanceArray721Ticket(wallet, tInfo);
        return checkBalanceArrayValidity(balance, token);
    }

    private List<BigInteger> checkBalanceArrayValidity(List<BigInteger> balance, Token token)
    {
        if (balance.size() > 0)
        {
            BigInteger firstVal = balance.get(0);
            if (firstVal.compareTo(BigInteger.valueOf(NODE_COMMS_ERROR)) == 0)
            {
                //comms error, use previous token balance
                if (token != null) balance = token.getArrayBalance();
                else balance.clear();
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

    private Single<Token[]> fetchStoredTokens(Wallet wallet) {
        return localSource
                .fetchTokensWithBalance(wallet);
    }

    private Single<Token> fetchCachedToken(int chainId, Wallet wallet, String address)
    {
        return localSource
                .fetchEnabledToken(chainId, wallet, address);
    }

    private BigDecimal updatePending(Token oldToken, BigDecimal pendingBalance)
    {
        if (!TokensService.getCurrentWalletAddress().equals(oldToken.getWallet()))
        {
            oldToken.pendingBalance = oldToken.balance;
        }
        else if (pendingBalance.equals(BigDecimal.valueOf(-1)))
        {
            oldToken.pendingBalance = oldToken.balance;
        }
        else
        {
            oldToken.pendingBalance = pendingBalance;
        }
        return pendingBalance;
    }

    private Single<Token> attachEth(NetworkInfo network, Wallet wallet, Token oldToken) {
        return getEthBalanceInternal(network, wallet, true)
                .map(pendingBalance -> updatePending(oldToken, pendingBalance))
                .flatMap(balance -> getEthBalanceInternal(network, wallet, false))
                .map(balance -> {
                    if (balance.equals(BigDecimal.valueOf(-1)))
                    {
                        oldToken.pendingBalance = oldToken.balance;
                        return oldToken;
                    }

                    if (!balance.equals(oldToken.balance))
                    {
                        Log.d(TAG, "Tx Update requested for: " + oldToken.getFullName());
                        TokenInfo info = new TokenInfo(wallet.address, network.name, network.symbol, 18, true,
                                                       network.chainId);
                        Token eth = new Token(info, balance, System.currentTimeMillis(), network.getShortName(), ContractType.ETHEREUM);
                        eth.setTokenWallet(wallet.address);
                        //store token and balance
                        localSource.updateTokenBalance(network, wallet, eth);
                        eth.transferPreviousData(oldToken);
                        eth.pendingBalance = balance;
                        eth.balanceChanged = true;
                        return eth;
                    }
                    else if (!balance.equals(oldToken.pendingBalance))
                    {
                        Log.d(TAG, "ETH: " + balance.toPlainString() + " OLD: " + oldToken.pendingBalance.toPlainString());
                        return oldToken;
                    }
                    else
                    {
                        return oldToken;
                    }
                })
                .flatMap(token -> localSource.fetchTicker(wallet, token)
                        .map(ticker -> ethereumNetworkRepository.updateTicker(token, ticker))
                        .map(ticker -> {
                            token.ticker = ticker;
                            return token;
                        })
                        .flatMap(ttoken -> localSource.saveTicker(wallet, ttoken))
                        .doOnError(throwable -> { System.out.println(throwable.getMessage()); })
                        .onErrorResumeNext(throwable -> Single.just(token)));
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
        currency.pendingBalance = BigDecimal.ZERO;
        return attachEth(network, wallet, currency);
    }

    @Override
    public Single<TokenTicker> getEthTicker(int chainId)
    {
        return ethereumNetworkRepository.getTicker(chainId);
    }

    @Override
    public Single<TokenTicker> getTokenTicker(Token token)
    {
        return Single.fromCallable(() -> ethereumNetworkRepository.getTokenTicker(token));
    }

    private BigDecimal getBalance(Wallet wallet, TokenInfo tokenInfo) throws Exception {
        Function function = balanceOf(wallet.address);
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
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
                DefaultBlockParameterName balanceCheckType = pending ? DefaultBlockParameterName.PENDING : DefaultBlockParameterName.LATEST;
                return new BigDecimal(getService(network.chainId).ethGetBalance(wallet.address, balanceCheckType)
                                                  .send()
                                                  .getBalance());
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

    private List<BigInteger> getBalanceArray875(Wallet wallet, TokenInfo tokenInfo) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            org.web3j.abi.datatypes.Function function = balanceOfArray(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
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

    private List<BigInteger> getBalanceArray721Ticket(Wallet wallet, TokenInfo tokenInfo) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            org.web3j.abi.datatypes.Function function = erc721TicketBalanceArray(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
            List<Type> tokenIds = callSmartContractFunctionArray(function, tokenInfo.address, network, wallet);
            if (tokenIds != null)
            {
                result.clear();
                for (Type val : tokenIds)
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

        if (responseValue == null || responseValue.equals("0x"))
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
                    if (!Utils.isAlNum(value)) value = "";
                    return (T)value;
                }
            }
            return (T) response.get(0).getValue();
        }
        else
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
                //now filter out any 'bad' chars
                name = filterAscii(name);
            }
        }

        return name;
    }

    private String filterAscii(String name)
    {
        StringBuilder sb = new StringBuilder();
        for (char ch : name.toCharArray())
        {
            if (ch >= 0x20 && ch <= 0x7E) //valid ASCII character
            {
                sb.append(ch);
            }
        }

        return sb.toString();
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
            return name;
        } else {
            return null;
        }
    }

    private int getDecimals(String address, NetworkInfo network) throws Exception {
        if (EthereumNetworkRepository.decimalOverride(address, network.chainId) > 0) return EthereumNetworkRepository.decimalOverride(address, network.chainId);
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

    private static org.web3j.abi.datatypes.Function erc721TicketBalanceArray(String owner) {
        return new org.web3j.abi.datatypes.Function(
                "getBalances",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    private static org.web3j.abi.datatypes.Function nameOf() {
        return new Function("name",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static org.web3j.abi.datatypes.Function supportsInterface(BigInteger value) {
        return new org.web3j.abi.datatypes.Function(
                "supportsInterface",
                Arrays.<Type>asList(new Bytes4(Numeric.toBytesPadded(value, 4))),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
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

    private static org.web3j.abi.datatypes.Function intParam(String param, BigInteger value) {
        return new Function(param,
                            Arrays.asList(new Uint256(value)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
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

    private static Function redeemed(BigInteger tokenId) throws NumberFormatException
    {
        return new Function(
                "redeemed",
                Collections.singletonList(new Uint256(tokenId)),
                Collections.singletonList(new TypeReference<Bool>() {}));
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

    public static byte[] createTicketTransferData(String to, List<BigInteger> tokenIndices, Token token) {
        Function function = token.getTransferFunction(to, tokenIndices);

        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createERC721TransferFunction(String to, Token token, List<BigInteger> tokenId)
    {
        Function function = token.getTransferFunction(to, tokenId);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createTrade(Token token, BigInteger expiry, List<BigInteger> ticketIndices, int v, byte[] r, byte[] s)
    {
        Function function = token.getTradeFunction(expiry, ticketIndices, v, r, s);
        String encodedFunction = FunctionEncoder.encode(function);
        return Numeric.hexStringToByteArray(Numeric.cleanHexPrefix(encodedFunction));
    }

    public static byte[] createSpawnPassTo(Token token, BigInteger expiry, List<BigInteger> tokenIds, int v, byte[] r, byte[] s, String recipient)
    {
        Function function = token.getSpawnPassToFunction(expiry, tokenIds, v, r, s, recipient);
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

    @Override
    public Single<ContractLocator> getTokenResponse(String address, int chainId, String method)
    {
        return Single.fromCallable(() -> {
            ContractLocator contractLocator = new ContractLocator(INVALID_CONTRACT, chainId);
            org.web3j.abi.datatypes.Function function = new Function(method,
                                                                     Arrays.<Type>asList(),
                                                                     Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));

            Wallet temp = new Wallet(null);
            String responseValue = callCustomNetSmartContractFunction(function, address, temp, chainId);
            if (responseValue == null) return contractLocator;

            List<Type> response = FunctionReturnDecoder.decode(
                    responseValue, function.getOutputParameters());
            if (response.size() == 1)
            {
                return new ContractLocator((String) response.get(0).getValue(), chainId);
            }
            else
            {
                return contractLocator;
            }
        });
    }

    private Single<TokenInfo> setupTokensFromLocal(String address, int chainId)
    {
        return Single.fromCallable(() -> {
            try
            {
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
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
    public Single<ContractType> determineCommonType(TokenInfo tokenInfo)
    {
        return Single.fromCallable(() -> {
            ContractType returnType = ContractType.OTHER;
            try
            {
                //could be either ERC721 or ERC20
                //try some interface values
                NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
                if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_BALANCES_721_TICKET), Boolean.TRUE)) returnType = ContractType.ERC721_TICKET;
                else if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_OFFICIAL_ERC721), Boolean.TRUE)) returnType = ContractType.ERC721;
                else if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_CRYPTOKITTIES), Boolean.TRUE)) returnType = ContractType.ERC721_LEGACY;
                else if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_OLD_ERC721), Boolean.TRUE)) returnType = ContractType.ERC721_LEGACY;
                else
                {
                    List<BigInteger> balance875 = checkERC875BalanceArray(new Wallet(ZERO_ADDRESS), tokenInfo, null);
                    List<BigInteger> balance721 = checkERC721TicketBalanceArray(new Wallet(ZERO_ADDRESS), tokenInfo, null);
                    String      responseValue = callSmartContractFunction(balanceOf(ZERO_ADDRESS), tokenInfo.address, network, new Wallet(ZERO_ADDRESS));
                    returnType = findContractTypeFromResponse(balance875, balance721, responseValue);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                //
            }

            return returnType;
        });
    }

    private ContractType findContractTypeFromResponse(List<BigInteger> balance875, List<BigInteger> balance721Ticket, String balanceResponse) throws Exception
    {
        ContractType returnType = ContractType.OTHER;

        int responseLength = balanceResponse.length();

        if (balance721Ticket != null && balance721Ticket.size() > 0)
        {
            returnType = ContractType.ERC721_TICKET;
        }
        else if (balance875 != null && balance875.size() > 0 && responseLength > 66)
        {
            returnType = ContractType.ERC875;
        }
        else if (balanceResponse.length() == 66) //expected biginteger size in hex + 0x
        {
            returnType = ContractType.ERC20;
        }

        return returnType;
    }

    @Override
    public Single<Boolean> fetchIsRedeemed(Token token, BigInteger tokenId)
    {
        return Single.fromCallable(() -> {
            boolean result = false;
            try
            {
                NetworkInfo networkInfo = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId);
                result = getContractData(networkInfo, token.tokenInfo.address, redeemed(tokenId), Boolean.TRUE);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        });
    }

    @Override
    public Single<String> resolveProxyAddress(TokenInfo tokenInfo)
    {
        return Single.fromCallable(() -> {
            String contractAddress = tokenInfo.address;
            try
            {
                NetworkInfo networkInfo = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
                String received = getContractData(networkInfo, tokenInfo.address, addrParam("implementation"), "");
                if (received != null && isValidAddress(received) && !received.equals(ZERO_ADDRESS))
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

    public static Web3j getWeb3jService(int chainId)
    {
        OkHttpClient okClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        HttpService publicNodeService = new HttpService(EthereumNetworkRepository.getNodeURLByNetworkId(chainId), okClient, false);
        EthereumNetworkRepository.addRequiredCredentials(chainId, publicNodeService);
        return Web3j.build(publicNodeService);
    }
}
