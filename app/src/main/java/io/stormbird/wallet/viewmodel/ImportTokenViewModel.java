package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;
import android.util.Log;

import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.CryptoFunctions;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.ServiceErrorException;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenFactory;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SetupTokensInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.FeeMasterService;

import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.ArrayList;
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

import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;
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

    private CryptoFunctions cryptoFunctions;
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

    private MagicLinkData importOrder;
    private String univeralImportLink;
    private Ticket importToken;
    private List<BigInteger> availableBalance = new ArrayList<>();
    private double ethToUsd = 0;

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
                         AssetDefinitionService assetDefinitionService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.feeMasterService = feeMasterService;
        this.addTokenInteract = addTokenInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.assetDefinitionService = assetDefinitionService;
    }

    private void initParser()
    {
        if (parser == null)
        {
            cryptoFunctions = new CryptoFunctions();
            parser = new ParseMagicLink(cryptoFunctions);
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
    public double getUSDPrice() { return ethToUsd; };

    public void prepare(String importDataStr) {
        univeralImportLink = importDataStr;
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onNetwork, this::onError);
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
    public Ticket getImportToken() { return importToken; }
    public MagicLinkData getSalesOrder() { return importOrder; }

    private void onNetwork(NetworkInfo networkInfo)
    {
        network.setValue(networkInfo);
        network.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onWallet, this::onError);
    }

    //1. Receive the default wallet (if any), then decode the import order
    private void onWallet(Wallet wallet) {
        initParser();
        this.wallet.setValue(wallet);
        try
        {
            importOrder = parser.parseUniversalLink(univeralImportLink);
            //ecrecover the owner
            importOrder.ownerAddress = parser.getOwnerKey(importOrder);
            //got to step 2. - get cached tokens
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
                    break;
                }
            }
        }

        loadToken();
    }

    public void loadToken()
    {
        fetchTokens();
        getEthereumTicker(); //simultaneously fetch the current eth price
    }

    //2. Fetch all cached tokens and get eth price
    private void fetchTokens() {
        importToken = null;
        disposable = fetchTokensInteract
                .fetchStoredToken(wallet.getValue(), importOrder.contractAddress)
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
            importToken = (Ticket) token;
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
                .update(contractAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTokensSetup, this::onError);
    }

    //4. Receive token information from blockchain query
    private void onTokensSetup(TokenInfo tokenInfo)
    {
        if (tokenInfo != null && tokenInfo.name != null) {
            TokenFactory tf = new TokenFactory();
            importToken = (Ticket)tf.createToken(tokenInfo);

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
        if (token != null && token instanceof Ticket)
        {
            importToken = (Ticket) token;
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
        List<BigInteger> newBalance = new ArrayList<>();
        for (Integer index : importOrder.tickets) //SalesOrder tickets member contains the list of ticket indices we're importing
        {
            if (importToken.balanceArray.size() > index) {
                BigInteger ticketId = importToken.balanceArray.get(index);
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
            invalidTime.setValue((int)validTime);
        }
        else if (balanceChange(newBalance))
        {
            availableBalance = newBalance;
            TicketRange range = new TicketRange(availableBalance.get(0), importToken.getAddress());
            for (int i = 1; i < availableBalance.size(); i++)
            {
                range.tokenIds.add(availableBalance.get(i));
            }
            importRange.setValue(range);
            regularBalanceCheck();
        }
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

//    //Store contract details if the contract is live,
//    //otherwise remove from the contract watch list
//    private void onTokensSetup(TokenInfo tokenInfo) {
//        //check this contract is good to add
//        if ((tokenInfo.name == null || tokenInfo.name.length() < 3)
//                || tokenInfo.isEnabled == false
//                || (tokenInfo.symbol == null || tokenInfo.symbol.length() < 2))
//        {
//            setupTokensInteract.putDeadContract(tokenInfo.address);
//        }
//        else {
//            disposable = addTokenInteract
//                    .add(tokenInfo)
//                    .subscribeOn(Schedulers.io())
//                    .subscribe(this::onSaved, this::onError);
//        }
//    }

    public void onError(Throwable throwable) {
        if (throwable.getCause() instanceof ServiceErrorException)
        {
            if (((ServiceErrorException) throwable.getCause()).code == C.ErrorCode.ALREADY_ADDED)
            {
                error.postValue(new ErrorEnvelope(C.ErrorCode.ALREADY_ADDED, null));
            }
        }
        else
        {
            error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
        }
    }

    public void performImport()
    {
        try
        {
            initParser();
            MagicLinkData order = parser.parseUniversalLink(univeralImportLink);
            //ok let's try to drive this guy through
            final byte[] tradeData = generateReverseTradeData(order);
            Log.d(TAG, "Approx value of trade: " + order.price);
            //now push the transaction
            disposable = createTransactionInteract
                    .create(wallet.getValue(), order.contractAddress, order.priceWei,
                            Contract.GAS_PRICE, Contract.GAS_LIMIT, tradeData)
                    .subscribe(this::onCreateTransaction, this::onError);

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
                    .subscribe(this::processFeemasterResult, this::onError);

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
                    error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Duplicate transaction passed."));
                    break;
                case 401:
                    error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Signature invalid."));
                    break;
                default:
                    error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "Transfer failed."));
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

    private void getEthereumTicker()
    {
        getTickerDisposable = fetchTokensInteract.getEthereumTicker()
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
            disposable = addTokenInteract.add(importToken.tokenInfo, wallet().getValue())
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
}