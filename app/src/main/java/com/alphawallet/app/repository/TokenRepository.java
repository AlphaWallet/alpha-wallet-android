package com.alphawallet.app.repository;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SubscribeWrapper;
import com.alphawallet.app.entity.TransferFromEventResponse;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.ERC721Ticket;
import com.alphawallet.app.entity.tokens.ERC721Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.service.AWHttpService;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsNetworkClientType;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.MagicLinkData;

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
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import okhttp3.OkHttpClient;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction;

public class TokenRepository implements TokenRepositoryType {

    private static final String TAG = "TRT";
    private final TokenLocalSource localSource;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final OkHttpClient okClient;
    private final Context context;
    private final TickerService tickerService;
    private final TransactionsNetworkClientType transactionClient;

    public static final String INVALID_CONTRACT = "<invalid>";

    public static final BigInteger INTERFACE_CRYPTOKITTIES = new BigInteger ("9a20483d", 16);
    public static final BigInteger INTERFACE_OFFICIAL_ERC721 = new BigInteger ("80ac58cd", 16);
    public static final BigInteger INTERFACE_OLD_ERC721 = new BigInteger ("6466353c", 16);
    public static final BigInteger INTERFACE_BALANCES_721_TICKET = new BigInteger ("c84aae17", 16);

    private static final int NODE_COMMS_ERROR = -1;
    private static final int CONTRACT_BALANCE_NULL = -2;

    private final Map<Integer, Web3j> web3jNodeServers;
    private AWEnsResolver ensResolver;

    public TokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenLocalSource localSource,
            OkHttpClient okClient,
            Context context,
            TickerService tickerService,
            TransactionsNetworkClientType networkClientType) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.localSource = localSource;
        this.ethereumNetworkRepository.addOnChangeDefaultNetwork(this::buildWeb3jClient);
        this.okClient = okClient;
        this.context = context;
        this.tickerService = tickerService;
        this.transactionClient = networkClientType;

        web3jNodeServers = new ConcurrentHashMap<>();
    }

    private void buildWeb3jClient(NetworkInfo networkInfo)
    {
        AWHttpService publicNodeService = new AWHttpService(networkInfo.rpcServerUrl, networkInfo.backupNodeUrl, okClient, false);
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

    // Only for sensing ERC721 Ticket
    @Override
    public Single<Token[]> checkInterface(Token[] tokens, Wallet wallet)
    {
        return Single.fromCallable(() -> {
            //check if the token interface has been checked
            for (int i = 0; i < tokens.length; i++)
            {
                Token t = tokens[i];
                if (t.getInterfaceSpec() == ContractType.ERC721_UNDETERMINED || !t.checkBalanceType()) //balance type appears to be wrong
                {
                    ContractType type = determineCommonType(t.tokenInfo).blockingGet();
                    TokenInfo tInfo = t.tokenInfo;
                    //upgrade type:
                    switch (type)
                    {
                        case OTHER:
                            //couldn't determine the type, try again next time
                            continue;
                        default:
                            type = ContractType.ERC721;
                        case ERC721:
                        case ERC721_LEGACY:
                            List<Asset> erc721Balance = t.getTokenAssets(); //add balance from Opensea
                            if (TextUtils.isEmpty(tInfo.name + tInfo.symbol)) tInfo = new TokenInfo(tInfo.address, " ", " ", tInfo.decimals, tInfo.isEnabled, tInfo.chainId); //ensure we don't keep overwriting this
                            t = new ERC721Token(tInfo, erc721Balance, System.currentTimeMillis(), t.getNetworkName(), type);
                            tokens[i] = t;
                            break;
                        case ERC721_TICKET:
                            List<BigInteger> balanceFromOpenSea = t.getArrayBalance();
                            t = new ERC721Ticket(t.tokenInfo, balanceFromOpenSea, System.currentTimeMillis(), t.getNetworkName(), ContractType.ERC721_TICKET);
                            tokens[i] = t;
                            break;
                    }

                    //update in database
                    updateTokenType(t, wallet, type);
                    TokensService.setInterfaceSpec(t.tokenInfo.chainId, t.tokenInfo.address, type);
                }
            }

            return tokens;
        });
    }

    @Override
    public Single<TokenCardMeta[]> fetchTokenMetas(Wallet wallet, List<Integer> networkFilters,
                                                   AssetDefinitionService svs)
    {
        if (networkFilters == null) networkFilters = Collections.emptyList(); //if filter null, return all networks
        return localSource
                .fetchTokenMetas(wallet, networkFilters, svs);
    }

    @Override
    public Single<TokenCardMeta[]> fetchAllTokenMetas(Wallet wallet, List<Integer> networkFilters, String searchTerm) {
        if (networkFilters == null) networkFilters = Collections.emptyList(); //if filter null, return all networks
        return localSource
                .fetchAllTokenMetas(wallet, networkFilters, searchTerm);
    }

    @Override
    public Realm getRealmInstance(Wallet wallet)
    {
        return localSource.getRealmInstance(wallet);
    }

    @Override
    public Realm getTickerRealmInstance()
    {
        return localSource.getTickerRealmInstance();
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
    public Token fetchToken(int chainId, String walletAddress, String address)
    {
        Wallet wallet = new Wallet(walletAddress);
        return localSource.fetchToken(chainId, wallet, address);
    }

    @Override
    public TokenTicker getTokenTicker(Token token)
    {
        return localSource.getCurrentTicker(token);
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
    public Single<BigDecimal> fetchChainBalance(String walletAddress, int chainId)
    {
        return Single.fromCallable(() -> updateNativeToken(new Wallet(walletAddress), chainId));
    }

    @Override
    public Single<Integer> fixFullNames(Wallet wallet, AssetDefinitionService svs)
    {
        return localSource.fixFullNames(wallet, svs);
    }

    @Override
    public Single<Boolean> updateTokenBalance(String walletAddress, int chainId, String tokenAddress, ContractType type)
    {
        Wallet wallet = new Wallet(walletAddress);
        return updateBalance(wallet, chainId, tokenAddress, type)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
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
    public Single<String> resolveENS(int chainId, String ensName)
    {
        if (ensResolver == null) ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), context);
        return ensResolver.resolveENSAddress(ensName);
    }

    @Override
    public Completable setEnable(Wallet wallet, Token token, boolean isEnabled)
    {
        NetworkInfo network = ethereumNetworkRepository.getDefaultNetwork();
        localSource.setEnable(network, wallet, token, isEnabled);
        return Completable.fromAction(() -> {});
    }

    @Override
    public Single<TokenInfo> update(String contractAddr, int chainId) {
        return setupTokensFromLocal(contractAddr, chainId);
    }

    @Override
    public Single<TokenInfo> update(String contractAddr, int chainId, boolean isEnabled) {
        return setupTokensFromLocal(contractAddr, chainId, isEnabled);
    }

    private Single<Boolean> updateBalance(final Wallet wallet, final int chainId, final String tokenAddress, ContractType type)
    {
        return Single.fromCallable(() -> {
                boolean hasBalanceChanged = false;
                try
                {
                    List<BigInteger> balanceArray = null;
                    BigDecimal balance = BigDecimal.valueOf(-1);

                    switch (type)
                    {
                        case ETHEREUM:
                            balance = getEthBalance(wallet, chainId);
                            break;
                        case ERC875:
                        case ERC875_LEGACY:
                            balanceArray = getBalanceArray875(wallet, chainId, tokenAddress);
                            break;
                        case ERC721_LEGACY:
                        case ERC721:
                            //checking raw balance, this only gives the count of tokens
                            balance = checkUint256Balance(wallet, chainId, tokenAddress);
                            break;
                        case ERC721_TICKET:
                            balanceArray = getBalanceArray721Ticket(wallet, chainId, tokenAddress);
                            break;
                        case ERC20:
                            balance = checkUint256Balance(wallet, chainId, tokenAddress);
                            break;
                        case NOT_SET:
                            break;
                        case OTHER:
                            //This token has its interface checked in the flow elsewhere
                            break;
                        default:
                            break;
                    }

                    if (!balance.equals(BigDecimal.valueOf(-1)) || balanceArray != null)
                    {
                        hasBalanceChanged = localSource.updateTokenBalance(wallet, chainId, tokenAddress, balance, balanceArray, type);
                    }

                    if (type != ContractType.ETHEREUM && wallet.address.equalsIgnoreCase(tokenAddress))
                    {
                        updateNativeToken(wallet, chainId);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                return hasBalanceChanged;
            });
    }

    /**
     * Used for an edge condition where you are looking at an account that's also contract
     *
     * @param wallet
     * @param chainId
     */
    private BigDecimal updateNativeToken(Wallet wallet, int chainId)
    {
        TokenFactory tFactory = new TokenFactory();
        NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
        TokenInfo tInfo = new TokenInfo("eth", network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = getEthBalance(wallet, chainId);
        if (!balance.equals(BigDecimal.valueOf(-1)))
        {
            Token nativeEthBackupToken = tFactory.createToken(tInfo, balance, null, System.currentTimeMillis(), ContractType.ETHEREUM, network.getShortName(), System.currentTimeMillis());
            localSource.updateTokenBalance(network, wallet, nativeEthBackupToken);
        }

        return balance;
    }

    /**
     * Obtain live balance of token from Ethereum blockchain and cache into Realm
     *
     * @param network
     * @param wallet
     * @param token
     * @return
     */
    private Single<Token> updateBalance(NetworkInfo network, Wallet wallet, final Token token)
    {
        if (token == null) return Single.fromCallable(() -> null);
        else return Single.fromCallable(() -> {
            TokenFactory tFactory = new TokenFactory();
            try
            {
                List<BigInteger> balanceArray = null;
                BigDecimal balance = BigDecimal.ZERO;
                TokenInfo tInfo = token.tokenInfo;
                ContractType interfaceSpec = token.getInterfaceSpec();

                switch (interfaceSpec)
                {
                    case ETHEREUM:
                        balance = getEthBalance(wallet, tInfo.chainId);
                        break;
                    case NOT_SET:
                        if (token.tokenInfo.name != null && token.tokenInfo.name.length() > 0)
                        {
                            Log.d(TAG, "NOT SET: " + token.getFullName());
                        }
                        break;
                    case ERC875:
                    case ERC875_LEGACY:
                        balanceArray = checkERC875BalanceArray(wallet, tInfo, token);
                        break;
                    case ERC721_LEGACY:
                    case ERC721:
                        break;
                    case ERC721_TICKET:
                        balanceArray = checkERC721TicketBalanceArray(wallet, tInfo, token);
                        break;
                    case ERC20:
                        balance = wrappedCheckUint256Balance(wallet, token.tokenInfo, token);
                        break;
                    case OTHER:
                        //This token has its interface checked in the flow elsewhere
                        break;
                    default:
                        break;
                }

                Token updated = tFactory.createToken(tInfo, balance, balanceArray, System.currentTimeMillis(), interfaceSpec, network.getShortName(), token.lastBlockCheck);
                localSource.updateTokenBalance(network, wallet, updated);
                updated.setTokenWallet(wallet.address);
                updated.balanceChanged = true;
                updated.pendingBalance = balance;
                return updated;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return token;
            }
        });
    }

    private BigDecimal checkUint256Balance(@NonNull Wallet wallet, int chainId, String tokenAddress)
    {
        BigDecimal balance = BigDecimal.valueOf(-1);

        try
        {
            Function function = balanceOf(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
            String responseValue = callSmartContractFunction(function, tokenAddress, network, wallet);

            if (!TextUtils.isEmpty(responseValue))
            {
                List<Type> response = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters());
                if (response.size() > 0) balance = new BigDecimal(((Uint256) response.get(0)).getValue());
            }
        }
        catch (Exception e)
        {
            //
        }

        return balance;
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

            if (token != null && TextUtils.isEmpty(responseValue))
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
            //use previous balance if appropriate
            if (token != null) balance = token.balance;
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

    @Override
    public Single<TokenTicker> getEthTicker(int chainId)
    {
        return Single.fromCallable(() -> tickerService.getEthTicker(chainId));
    }

    private BigDecimal getEthBalance(Wallet wallet, int chainId)
    {
        try {
            return new BigDecimal(getService(chainId).ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST)
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

    private List<BigInteger> getBalanceArray875(Wallet wallet, int chainId, String tokenAddress) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            Function function = balanceOfArray(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
            List<Type> indices = callSmartContractFunctionArray(function, tokenAddress, network, wallet);
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

    private List<BigInteger> getBalanceArray875(Wallet wallet, TokenInfo tokenInfo) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            Function function = balanceOfArray(wallet.address);
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

    private List<BigInteger> getBalanceArray721Ticket(Wallet wallet, int chainId, String tokenAddress) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            Function function = erc721TicketBalanceArray(wallet.address);
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
            List<Type> tokenIds = callSmartContractFunctionArray(function, tokenAddress, network, wallet);
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

    private List<BigInteger> getBalanceArray721Ticket(Wallet wallet, TokenInfo tokenInfo) {
        List<BigInteger> result = new ArrayList<>();
        result.add(BigInteger.valueOf(NODE_COMMS_ERROR));
        try
        {
            Function function = erc721TicketBalanceArray(wallet.address);
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

    private <T> T getContractData(NetworkInfo network, String address, Function function, T type) throws Exception
    {
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, network, temp);

        if (TextUtils.isEmpty(responseValue))
        {
            throw new Exception("Bad contract value");
        }
        else if (responseValue.equals("0x"))
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
        Function function = nameOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, network ,temp);

        if (TextUtils.isEmpty(responseValue)) return null;

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
        Function function = decimalsOf();
        Wallet temp = new Wallet(null);
        String responseValue = callSmartContractFunction(function, address, network, temp);
        if (TextUtils.isEmpty(responseValue)) return 18;

        List<Type> response = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters());
        if (response.size() == 1) {
            return ((Uint8) response.get(0)).getValue().intValue();
        } else {
            return 18;
        }
    }

    private static Function balanceOf(String owner) {
        return new Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<Uint256>() {}));
    }

    private static Function balanceOfArray(String owner) {
        return new Function(
                "balanceOf",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    private static Function erc721TicketBalanceArray(String owner) {
        return new Function(
                "getBalances",
                Collections.singletonList(new Address(owner)),
                Collections.singletonList(new TypeReference<DynamicArray<Uint256>>() {}));
    }

    private static Function nameOf() {
        return new Function("name",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static Function supportsInterface(BigInteger value) {
        return new Function(
                "supportsInterface",
                Arrays.<Type>asList(new Bytes4(Numeric.toBytesPadded(value, 4))),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
    }

    private static Function stringParam(String param) {
        return new Function(param,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static Function boolParam(String param) {
        return new Function(param,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
    }

    private static Function stringParam(String param, BigInteger value) {
        return new Function(param,
                            Arrays.asList(new Uint256(value)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static Function intParam(String param, BigInteger value) {
        return new Function(param,
                            Arrays.asList(new Uint256(value)),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    }

    private static Function intParam(String param) {
        return new Function(param,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint>() {}));
    }

    private static Function symbolOf() {
        return new Function("symbol",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    }

    private static Function decimalsOf() {
        return new Function("decimals",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
    }

    private static Function addrParam(String param) {
        return new Function(param,
                            Arrays.<Type>asList(),
                            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    }

    private Function addressFunction(String method, byte[] resultHash)
    {
        return new Function(
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
            Function function, String contractAddress, NetworkInfo network, Wallet wallet)
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
            Function function, String contractAddress, NetworkInfo network, Wallet wallet) throws Exception
    {
        try
        {
            String encodedFunction = FunctionEncoder.encode(function);

            org.web3j.protocol.core.methods.request.Transaction transaction
                    = createEthCallTransaction(wallet.address, contractAddress, encodedFunction);
            EthCall response = getService(network.chainId).ethCall(transaction, DefaultBlockParameterName.LATEST).send();

            return response.getValue();
        }
        catch (InterruptedIOException|UnknownHostException e)
        {
            //expected to happen when user switches wallets
            return "0x";
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
            Function function = new Function(method,
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

    private Single<TokenInfo> setupTokensFromLocal(String address, int chainId) //pass exception up the chain
    {
        return Single.fromCallable(() -> {
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
            return new TokenInfo(
                    address,
                    getName(address, network),
                    getContractData(network, address, stringParam("symbol"), ""),
                    getDecimals(address, network),
                    true,
                    chainId);
        });
    }

    /*
     * This method is introduced to set "Enabled" for popular tokens as can't enable all tokens.
     */
    private Single<TokenInfo> setupTokensFromLocal(String address, int chainId, boolean isEnabled) //pass exception up the chain
    {
        return Single.fromCallable(() -> {
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(chainId);
            return new TokenInfo(
                    address,
                    getName(address, network),
                    getContractData(network, address, stringParam("symbol"), ""),
                    getDecimals(address, network),
                    isEnabled,
                    chainId);
        });
    }

    @Override
    public Single<ContractType> determineCommonType(TokenInfo tokenInfo)
    {
        return Single.fromCallable(() -> {
            ContractType returnType;

            //could be ERC721, ERC721T, ERC875 or ERC20
            //try some interface values
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
            try
            {
                if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_BALANCES_721_TICKET), Boolean.TRUE))
                    returnType = ContractType.ERC721_TICKET;
                else if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_OFFICIAL_ERC721), Boolean.TRUE))
                    returnType = ContractType.ERC721;
                else if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_CRYPTOKITTIES), Boolean.TRUE))
                    returnType = ContractType.ERC721_LEGACY;
                else if (getContractData(network, tokenInfo.address, supportsInterface(INTERFACE_OLD_ERC721), Boolean.TRUE))
                    returnType = ContractType.ERC721_LEGACY;
                else
                    returnType = ContractType.OTHER;
            }
            catch (Exception e)
            {
                returnType = ContractType.OTHER;
            }

            if (returnType == ContractType.OTHER)
            {
                Boolean isERC875;
                String      responseValue;

                try
                {
                    isERC875 = getContractData(network, tokenInfo.address, boolParam("isStormBirdContract"), Boolean.TRUE); //Use old isStormbird as another datum point
                }
                catch (Exception e) { isERC875 = false; }
                try
                {
                    responseValue = callSmartContractFunction(balanceOf(ZERO_ADDRESS), tokenInfo.address, network, new Wallet(ZERO_ADDRESS));
                }
                catch (Exception e) { responseValue = ""; }

                returnType = findContractTypeFromResponse(responseValue, isERC875);
            }

            return returnType;
        }).flatMap(type -> additionalHandling(type, tokenInfo));
    }

    private Single<ContractType> queryInterfaceSpec(String address, TokenInfo tokenInfo)
    {
        NetworkInfo networkInfo = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
        ContractType checked = TokensService.checkInterfaceSpec(tokenInfo.chainId, tokenInfo.address);
        if (tokenInfo.name == null && tokenInfo.symbol == null)
        {
            return Single.fromCallable(() -> ContractType.NOT_SET);
        }
        else if (checked != null && checked != ContractType.NOT_SET && checked != ContractType.OTHER)
        {
            return Single.fromCallable(() -> checked);
        }
        else return Single.fromCallable(() -> ContractType.OTHER);
    }

    public Single<ContractType> additionalHandling(ContractType type, TokenInfo tokenInfo)
    {
        switch (type)
        {
            case ERC20:
            case ERC721:
            case ERC721_LEGACY:
            case ERC721_TICKET:
                return Single.fromCallable(() -> type);
            case ERC875:
                //requires additional handling to determine if it's Legacy type, but safe to return ERC875 for now:
                queryInterfaceSpec(tokenInfo.address, tokenInfo)
                        .subscribeOn(Schedulers.io())
                        .subscribe(actualType -> TokensService.setInterfaceSpec(tokenInfo.chainId, tokenInfo.address, actualType)).isDisposed();
                return Single.fromCallable(() -> type);
            default:
                return Single.fromCallable(() -> type); //take no further action: possible that this is not a valid token
        }
    }

    private ContractType findContractTypeFromResponse(String balanceResponse, Boolean isERC875) throws Exception
    {
        ContractType returnType = ContractType.OTHER;

        int responseLength = balanceResponse.length();

        if (isERC875 || (responseLength > 66))
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
            NetworkInfo networkInfo = ethereumNetworkRepository.getNetworkByChain(token.tokenInfo.chainId);
            return getContractData(networkInfo, token.tokenInfo.address, redeemed(tokenId), Boolean.TRUE);
        });
    }

    @Override
    public Disposable addImageUrl(int networkId, String address, String imageUrl)
    {
        return localSource.storeTokenUrl(networkId, address, imageUrl);
    }

    public static Web3j getWeb3jService(int chainId)
    {
        OkHttpClient okClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        AWHttpService publicNodeService = new AWHttpService(EthereumNetworkRepository.getNodeURLByNetworkId (chainId), EthereumNetworkRepository.getSecondaryNodeURL(chainId), okClient, false);
        EthereumNetworkRepository.addRequiredCredentials(chainId, publicNodeService);
        return Web3j.build(publicNodeService);
    }
}
