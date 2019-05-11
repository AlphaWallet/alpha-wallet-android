package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import io.reactivex.Single;
import io.stormbird.token.tools.Numeric;
import io.stormbird.token.tools.TokenDefinition;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.FeeMasterService;

import io.stormbird.wallet.ui.ImportTokenActivity;
import org.web3j.crypto.Sign;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.entity.SalesOrderMalformed;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.tools.ParseMagicLink;

import static io.stormbird.token.tools.ParseMagicLink.*;
import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;
import static io.stormbird.wallet.entity.CryptoFunctions.sigFromByteArray;
import static io.stormbird.wallet.entity.MagicLinkParcel.generateReverseTradeData;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModel extends BaseViewModel
{
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final String TAG = "ITVM";

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final FeeMasterService feeMasterService;
    private final AddTokenInteract addTokenInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;

    private ParseMagicLink parser;

    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> network = new MutableLiveData<>();
    private final MutableLiveData<TicketRange> importRange = new MutableLiveData<>();
    private final MutableLiveData<Integer> invalidRange = new MutableLiveData<>();
    private final MutableLiveData<Integer> invalidTime = new MutableLiveData<>();
    private final MutableLiveData<Boolean> invalidLink = new MutableLiveData<>();
    private final MutableLiveData<String> checkContractNetwork = new MutableLiveData<>();
    private final MutableLiveData<Boolean> ticketNotValid = new MutableLiveData<>();
    private final MutableLiveData<Boolean> feemasterAvailable = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> txError = new MutableLiveData<>();

    private MagicLinkData importOrder;
    private String univeralImportLink;
    private Token importToken;
    private List<BigInteger> availableBalance = new ArrayList<>();
    private double ethToUsd = 0;
    private TicketRange currentRange;
    private int networkCount;
    private boolean foundNetwork;

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable getTickerDisposable;

    ImportTokenViewModel(FindDefaultNetworkInteract findDefaultNetworkInteract,
                         FindDefaultWalletInteract findDefaultWalletInteract,
                         CreateTransactionInteract createTransactionInteract,
                         FetchTokensInteract fetchTokensInteract,
                         SetupTokensInteract setupTokensInteract,
                         FeeMasterService feeMasterService,
                         AddTokenInteract addTokenInteract,
                         EthereumNetworkRepositoryType ethereumNetworkRepository,
                         AssetDefinitionService assetDefinitionService,
                         FetchTransactionsInteract fetchTransactionsInteract,
                         FetchGasSettingsInteract fetchGasSettingsInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.feeMasterService = feeMasterService;
        this.addTokenInteract = addTokenInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
    }

    private void initParser()
    {
        if (parser == null)
        {
            parser = new ParseMagicLink(new CryptoFunctions());
        }
    }

    public LiveData<TicketRange> importRange() {
        return importRange;
    }
    public LiveData<Integer> invalidRange() { return invalidRange; }
    public LiveData<Integer> invalidTime() { return invalidTime; }
    public LiveData<String> newTransaction() { return newTransaction; }
    public LiveData<Boolean> invalidLink() { return invalidLink; }
    public LiveData<String> checkContractNetwork() { return checkContractNetwork; }
    public LiveData<Boolean> ticketNotValid() { return ticketNotValid; }
    public LiveData<Boolean> feemasterAvailable() { return feemasterAvailable; }
    public LiveData<ErrorEnvelope> txError() { return txError; }
    public double getUSDPrice() { return ethToUsd; }

    public void prepare(String importDataStr) {
        univeralImportLink = importDataStr;
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onWallet, this::onError);
    }

    @Override
    protected void onCleared()
    {
        super.onCleared();
        if (getBalanceDisposable != null)
        {
            getBalanceDisposable.dispose();
        }
    }

    public LiveData<Wallet> wallet() {
        return wallet;
    }
    public LiveData<NetworkInfo> network()
    {
        return network;
    }
    public Token getImportToken() { return importToken; }
    public MagicLinkData getSalesOrder() { return importOrder; }

    //1. Receive the default wallet (if any), then decode the import order
    private void onWallet(Wallet wallet) {
        initParser();
        this.wallet.setValue(wallet);
        try
        {
            currentRange = null;
            importOrder = parser.parseUniversalLink(univeralImportLink);
            //ecrecover the owner
            importOrder.ownerAddress = parser.getOwnerKey(importOrder);
            //see if we picked up a network from the link
            checkContractNetwork.postValue(importOrder.contractAddress);
        }
        catch (SalesOrderMalformed e)
        {
            invalidLink.postValue(true);
        }
        catch (Exception e)
        {
            invalidLink.postValue(true);
        }
    }

    public void switchNetwork(int newNetwork)
    {
        if (network.getValue() == null || network.getValue().chainId != newNetwork)
        {
            NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
            for (NetworkInfo networkInfo : networks)
            {
                if (networkInfo.chainId == newNetwork)
                {
                    ethereumNetworkRepository.setDefaultNetworkInfo(networkInfo);
                    network.setValue(networkInfo);
                    return;
                }
            }
            invalidLink.postValue(true);
        }
    }

    public void loadToken()
    {
        switch (importOrder.contractType)
        {
            case currencyLink:
                updateToken(); //don't check contract
                break;
            default:
                fetchTokens();
                break;
        }

        getEthereumTicker(network.getValue().chainId); //simultaneously fetch the current eth price
    }

    //2. Fetch all cached tokens and get eth price
    private void fetchTokens() {
        importToken = null;
        disposable = fetchTokensInteract
                .fetchStoredToken(network.getValue(), wallet.getValue(), importOrder.contractAddress)
                .subscribe(this::onToken, this::onFetchError, this::fetchTokensComplete);
    }

    private void onFetchError(Throwable throwable)
    {
        //there was no token found, retrieve from blockchain
        setupTokenAddr(importOrder.contractAddress);
    }

    private void onToken(Token token)
    {
        if (token.addressMatches(importOrder.contractAddress) && (token instanceof Ticket))
        {
            importToken = token;
            regularBalanceCheck(); //fetch balance and display
        }
    }

    //2b. on completion of receiving tokens check if we found the matching token
    private void fetchTokensComplete()
    {
        if (importToken == null)
        {
            //Didn't have the token cached, so retrieve it from blockchain
            setupTokenAddr(importOrder.contractAddress);
        }
    }

    //3. If token not already cached we need to fetch details from the ethereum contract itself
    private void setupTokenAddr(String contractAddress)
    {
        disposable = setupTokensInteract
                .update(contractAddress, network().getValue().chainId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::getTokenSpec, this::onError);
    }

    private void getTokenSpec(TokenInfo info)
    {
        disposable = fetchTransactionsInteract.queryInterfaceSpec(info)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(contractSpec -> onTokensSetup(info, contractSpec), this::onError);
    }

    //4. Receive token information from blockchain query
    private void onTokensSetup(TokenInfo tokenInfo, ContractType spec)
    {
        if (tokenInfo != null && tokenInfo.name != null)
        {
            TokenFactory tf = new TokenFactory();
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId, false);
            importToken = tf.createToken(tokenInfo, spec, network.getShortName());
            regularBalanceCheck();
        }
        else
        {
            ticketNotValid.postValue(true);
            //invalidLink.postValue(true);
        }
    }

    //4a. Receive balance
    private void onBalance(Token token)
    {
        if (token.tokenInfo != null)
        {
            importToken = token;
        }
        else if (getBalanceDisposable != null && !getBalanceDisposable.isDisposed())
        {
            getBalanceDisposable.dispose();
        }
    }

    //4b. update token information with balance
    private void gotBalance()
    {
        if (importToken != null)
        {
            updateToken();
        }
        else
        {
            invalidLink.postValue(true);
        }
    }

    //5. We have token information and balance. Check if the import order is still valid.
    private void updateToken()
    {
        switch (importOrder.contractType)
        {
            case spawnable:
                //TODO: Check if this spawnable is still valid.
                //      Can we determine if this is the case by inquiring a contract method?
                currentRange = new TicketRange(importOrder.tokenIds.get(0), importToken.getAddress());
                for (int i = 1; i < importOrder.tokenIds.size(); i++)
                {
                    currentRange.tokenIds.add(importOrder.tokenIds.get(i));
                }
                importRange.setValue(currentRange);
                break;
            case currencyLink:
                //setup UI for currency import
                currentRange = null;
                importRange.setValue(currentRange);
                break;
            case unassigned:
            case normal:
            case customizable:
                List<BigInteger> newBalance = new ArrayList<>();
                for (Integer index : importOrder.tickets) //SalesOrder tickets member contains the list of ticket indices we're importing
                {
                    if (importToken.getArrayBalance().size() > index)
                    {
                        BigInteger ticketId = importToken.getArrayBalance().get(index);
                        if (ticketId.compareTo(BigInteger.ZERO) != 0)
                        {
                            newBalance.add(ticketId); //ticket is there
                        }
                    }
                }

                long validTime = checkExpiry();

                if (newBalance.size() == 0 || newBalance.size() != importOrder.tickets.length)
                {
                    //tickets already imported
                    invalidRange.setValue(newBalance.size());
                }
                else if (validTime < 0)
                {
                    invalidTime.setValue((int) validTime);
                }
                else if (balanceChange(newBalance))
                {
                    availableBalance = newBalance;
                    currentRange = new TicketRange(availableBalance.get(0), importToken.getAddress());
                    for (int i = 1; i < availableBalance.size(); i++)
                    {
                        currentRange.tokenIds.add(availableBalance.get(i));
                    }
                    determineInterface();
                }
                break;
        }
    }

    private void determineInterface()
    {
        if (importToken.unspecifiedSpec())
        {
            //establish the interface spec
            disposable = fetchTransactionsInteract.queryInterfaceSpec(importToken.tokenInfo)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onInterfaceSpec, this::onError);
        }
        else
        {
            importRange.setValue(currentRange);
            regularBalanceCheck();
        }
    }

    private void onInterfaceSpec(ContractType spec)
    {
        importToken.setInterfaceSpec(spec);
        importRange.setValue(currentRange);
        regularBalanceCheck();
    }

    private long checkExpiry()
    {
        //get current UNIX
        long UTCTimeStamp = System.currentTimeMillis() / 1000;

        //has the ticket expired already?
        return (importOrder.expiry - UTCTimeStamp);
    }

    //perform a balance check cycle every CHECK_BALANCE_INTERVAL seconds
    private void regularBalanceCheck()
    {
        long validTime = checkExpiry();
        if (validTime < 0)
        {
            invalidTime.setValue((int)validTime);
        }
        else
        {
            getBalanceDisposable = Observable.interval(0, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                    .doOnNext(l -> fetchTokensInteract
                            .updateBalance(importOrder.ownerAddress, importToken)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onBalance, this::onError, this::gotBalance)).subscribe();
        }
    }

    public void onTransactionError(Throwable throwable) {
        if (throwable.getCause() instanceof ServiceErrorException)
        {
            if (((ServiceErrorException) throwable.getCause()).code == C.ErrorCode.ALREADY_ADDED)
            {
                txError.postValue(new ErrorEnvelope(C.ErrorCode.ALREADY_ADDED, null));
            }
        }
        else
        {
            txError.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
        }
    }

    public void performImport()
    {
        try
        {
            initParser();
            MagicLinkData order = parser.parseUniversalLink(univeralImportLink);
            //calculate gas settings
            final byte[] tradeData = generateReverseTradeData(order, importToken, wallet.getValue().address);
            disposable = fetchGasSettingsInteract
                    .fetch(tradeData, true, importOrder.chainId)
                    .subscribe(this::performImportFinal, this::onTransactionError);
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Import Error."));
        }
    }

    private void performImportFinal(GasSettings settings)
    {
        try
        {
            MagicLinkData order = parser.parseUniversalLink(univeralImportLink);
            //ok let's try to drive this guy through
            final byte[] tradeData = generateReverseTradeData(order, importToken, wallet.getValue().address);
            Log.d(TAG, "Approx value of trade: " + order.price);
            //now push the transaction
            disposable = createTransactionInteract
                    .create(wallet.getValue(), order.contractAddress, order.priceWei,
                            settings.gasPrice, settings.gasLimit, tradeData, order.chainId)
                    .subscribe(this::onCreateTransaction, this::onTransactionError);

            addTokenWatchToWallet();
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Import Error."));
        }
    }

    public void importThroughFeemaster(String url)
    {
        try
        {
            initParser();
            MagicLinkData order = parser.parseUniversalLink(univeralImportLink);
            disposable = feeMasterService.handleFeemasterImport(url, wallet.getValue(), network.getValue().chainId, order)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::processFeemasterResult, this::onTransactionError);

            addTokenWatchToWallet();
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Import Error."));
        }
    }

    private void processFeemasterResult(Integer result)
    {
        if ((result/100) == 2) newTransaction.postValue("Transaction accepted by server.");
        else
        {
            switch (result)
            {
                case 501:
                    txError.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Duplicate transaction passed."));
                    break;
                case 401:
                    txError.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Signature invalid."));
                    break;
                default:
                    txError.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Transfer failed."));
                    break;
            }
        }
    }

    private void onCreateTransaction(String transaction)
    {
        newTransaction.postValue(transaction);
    }

    private boolean balanceChange(List<BigInteger> newBalance)
    {
        return !(newBalance.containsAll(availableBalance) && availableBalance.containsAll(newBalance));
    }

    private void getEthereumTicker(int chainId)
    {
        getTickerDisposable = fetchTokensInteract.getEthereumTicker(chainId)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTicker, this::onError);
    }

    private void onTicker(Ticker ticker)
    {
        if (ticker != null && ticker.price_usd != null)
        {
            ethToUsd = Double.valueOf(ticker.price_usd);
        }
    }

    private void addTokenWatchToWallet()
    {
        if (importToken != null)
        {
            disposable = addTokenInteract.add(importToken.tokenInfo, importToken.getInterfaceSpec(), wallet.getValue())
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::finishedImport, this::onError);
        }
    }

    private void finishedImport(Token token)
    {
        Log.d(TAG, "Added to Watch list: " + token.getFullName());
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void checkFeemaster(String feemasterServer)
    {
        disposable = feeMasterService.checkFeemasterService(feemasterServer, network.getValue().chainId, importOrder.contractAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFeemasterAvailability, this::onFeeMasterError);
    }

    private void onFeeMasterError(Throwable throwable)
    {
        feemasterAvailable.postValue(false);
    }

    private void handleFeemasterAvailability(Boolean available)
    {
        feemasterAvailable.postValue(available);
    }

    public void checkTokenNetwork(String contractAddress, String method)
    {
        //determine which network the contract is on.
        //first try the current default
        if (network.getValue() != null)
        {
            //try this network
            disposable = fetchTokensInteract.getContractResponse(contractAddress, network.getValue().chainId, method)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(response -> tryDefault(response, method), this::onError);
        }
        else
        {
            testNetworks(method);
        }
    }

    private void testNetworks(String method)
    {
        foundNetwork = false;
        networkCount = ethereumNetworkRepository.getAvailableNetworkList().length;
        //test all the networks

        disposable = Observable.fromCallable(this::getNetworkIds)
                .flatMapIterable(networkId -> networkId)
                .filter(networkId -> !foundNetwork)
                .flatMap(networkId -> fetchTokensInteract.getContractResponse(importOrder.contractAddress, networkId, method))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::testNetworkResult, this::onTestError);
    }

    private void onTestError(Throwable throwable)
    {
        checkNetworkCount();
        onError(throwable);
    }

    private List<Integer> getNetworkIds()
    {
        List<Integer> networkIds = new ArrayList<>();
        for (NetworkInfo networkInfo : ethereumNetworkRepository.getAvailableNetworkList())
        {
            networkIds.add(networkInfo.chainId);
        }
        return networkIds;
    }

    private void testNetworkResult(ContractResult result)
    {
        if (!foundNetwork && !result.name.equals(TokenRepository.INVALID_CONTRACT))
        {
            foundNetwork = true;
            switchNetwork(result.chainId);
            loadToken();
        }
        else
        {
            checkNetworkCount();
        }
    }

    private void checkNetworkCount()
    {
        networkCount--;
        if (networkCount == 0)
        {
            invalidLink.postValue(true);
        }
    }

    private void tryDefault(ContractResult result, String method)
    {
        if (result.name.equals(TokenRepository.INVALID_CONTRACT))
        {
            testNetworks(method);
        }
        else
        {
            loadToken(); //proceed
        }
    }

    public NetworkInfo getNetwork()
    {
        return network.getValue();
    }
}