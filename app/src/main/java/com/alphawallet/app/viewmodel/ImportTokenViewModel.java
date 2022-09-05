package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.entity.MagicLinkParcel.generateReverseTradeData;
import static com.alphawallet.token.tools.ParseMagicLink.currencyLink;
import static com.alphawallet.token.tools.ParseMagicLink.customizable;
import static com.alphawallet.token.tools.ParseMagicLink.normal;
import static com.alphawallet.token.tools.ParseMagicLink.spawnable;
import static com.alphawallet.token.tools.ParseMagicLink.unassigned;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokendata.TokenTicker;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.service.AlphaWalletService;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.ParseMagicLink;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by James on 9/03/2018.
 */
@HiltViewModel
public class ImportTokenViewModel extends BaseViewModel
{
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final String TAG = "ITVM";

    private final GenericWalletInteract genericWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final TokensService tokensService;
    private final AlphaWalletService alphaWalletService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final GasService gasService;
    private final KeyService keyService;

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
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();
    private final MutableLiveData<TokenTicker> tickerUpdate = new MutableLiveData<>();

    private MagicLinkData importOrder;
    private String universalImportLink;
    private Token importToken;
    private List<BigInteger> availableBalance = new ArrayList<>();
    private TicketRange currentRange;
    private int networkCount;
    private boolean foundNetwork;

    @Nullable
    private Disposable getBalanceDisposable;

    @Inject
    ImportTokenViewModel(GenericWalletInteract genericWalletInteract,
                         CreateTransactionInteract createTransactionInteract,
                         FetchTokensInteract fetchTokensInteract,
                         TokensService tokensService,
                         AlphaWalletService alphaWalletService,
                         EthereumNetworkRepositoryType ethereumNetworkRepository,
                         AssetDefinitionService assetDefinitionService,
                         FetchTransactionsInteract fetchTransactionsInteract,
                         GasService gasService,
                         KeyService keyService) {
        this.genericWalletInteract = genericWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.tokensService = tokensService;
        this.alphaWalletService = alphaWalletService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.gasService = gasService;
        this.keyService = keyService;
    }

    private void initParser()
    {
        if (parser == null)
        {
            parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
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
    public LiveData<XMLDsigDescriptor> sig() { return sig; }
    public LiveData<TokenTicker> tickerUpdate() { return tickerUpdate; }

    public void prepare(String importDataStr) {
        universalImportLink = importDataStr;
        disposable = genericWalletInteract
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
            importOrder = parser.parseUniversalLink(universalImportLink);
            //ecrecover the owner
            importOrder.ownerAddress = parser.getOwnerKey(importOrder);
            //see if we picked up a network from the link
            checkContractNetwork.postValue(importOrder.contractAddress);
        }
        catch (Exception e)
        {
            invalidLink.postValue(true);
        }
    }

    public void switchNetwork(long newNetwork)
    {
        if (network.getValue() == null || network.getValue().chainId != newNetwork)
        {
            NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
            for (NetworkInfo networkInfo : networks)
            {
                if (networkInfo.chainId == newNetwork)
                {
                    ethereumNetworkRepository.setActiveBrowserNetwork(networkInfo);
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
                fetchToken();
                break;
        }

        gasService.startGasPriceCycle(importOrder.chainId); //start fetching gas price
    }

    //2. Fetch all cached tokens and get eth price
    private void fetchToken() {
        importToken = tokensService.getToken(importOrder.chainId, importOrder.contractAddress);
        if (importToken != null && !(importToken.getInterfaceSpec() == ContractType.NOT_SET || importToken.getInterfaceSpec() == ContractType.OTHER))
        {
            regularBalanceCheck(); //fetch balance and display
        }
        else
        {
            setupTokenAddr(importOrder.contractAddress);
        }
    }

    //3. If token not already cached we need to fetch details from the ethereum contract itself
    private void setupTokenAddr(String contractAddress)
    {
        disposable = tokensService
                .update(contractAddress, importOrder.chainId, ContractType.NOT_SET)
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
            NetworkInfo network = ethereumNetworkRepository.getNetworkByChain(tokenInfo.chainId);
            importToken = tf.createToken(tokenInfo, spec, network.getShortName());
            regularBalanceCheck();
        }
        else
        {
            ticketNotValid.postValue(true);
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
                getEthereumTicker(importOrder.chainId);
                break;
            case currencyLink:
                //setup UI for currency import
                currentRange = null;
                importRange.setValue(currentRange);
                getEthereumTicker(importOrder.chainId);
                break;
            case unassigned:
            case normal:
            case customizable:
                List<BigInteger> newBalance = new ArrayList<>();
                for (Integer index : importOrder.indices) //SalesOrder indices member contains the list of ticket indices we're importing
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

                if (newBalance.size() == 0 || newBalance.size() != importOrder.indices.length)
                {
                    //indices already imported
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
                getEthereumTicker(importOrder.chainId);
                break;
        }
    }

    private void determineInterface()
    {
        if (!importToken.contractTypeValid())
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
        txError.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
    }

    public void performImport()
    {
        try
        {
            initParser();
            MagicLinkData order = parser.parseUniversalLink(universalImportLink);
            //calculate gas settings
            final byte[] tradeData = generateReverseTradeData(order, importToken, wallet.getValue().address);

            gasService.calculateGasEstimate(tradeData, importOrder.chainId, wallet.getValue().address, order.amount, wallet.getValue(), BigInteger.ZERO)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::performImportFinal).isDisposed();
        }
        catch (Exception e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
            error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "Import Error."));
        }
    }

    private void performImportFinal(BigInteger gasEstimate)
    {
        try
        {
            MagicLinkData order = parser.parseUniversalLink(universalImportLink);
            //ok let's try to drive this guy through
            final byte[] tradeData = generateReverseTradeData(order, importToken, wallet.getValue().address);
            Timber.tag(TAG).d("Approx value of trade: %s", order.price);
            //now push the transaction
            disposable = createTransactionInteract
                    .create(wallet.getValue(), order.contractAddress, order.priceWei,
                            gasService.getGasPrice(), gasEstimate, tradeData, order.chainId)
                    .subscribe(this::onCreateTransaction, this::onTransactionError);

            addTokenWatchToWallet();
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
            error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "Import Error."));
        }
    }

    public void importThroughFeemaster(String url)
    {
        try
        {
            initParser();
            MagicLinkData order = parser.parseUniversalLink(universalImportLink);
            disposable = alphaWalletService.handleFeemasterImport(url, wallet.getValue(), network.getValue().chainId, order)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::processFeemasterResult, this::onTransactionError);

            addTokenWatchToWallet();
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
            error.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "Import Error."));
        }
    }

    private void processFeemasterResult(Integer result)
    {
        if ((result/100) == 2) newTransaction.postValue("Transaction accepted by server.");
        else
        {
            switch (result)
            {
                case 400:
                    txError.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "Token already claimed."));
                    break;
                case 401:
                    txError.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "Signature invalid."));
                    break;
                default:
                    txError.postValue(new ErrorEnvelope(C.ErrorCode.EMPTY_COLLECTION, "Transfer failed."));
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

    private void getEthereumTicker(long chainId)
    {
        disposable = fetchTokensInteract.getEthereumTicker(chainId)
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTicker, this::onError);
    }

    private void onTicker(TokenTicker ticker)
    {
        if (ticker != null && ticker.updateTime > 0)
        {
            tickerUpdate.postValue(ticker);
        }
    }

    private void addTokenWatchToWallet()
    {
        if (importToken != null)
        {
            tokensService.storeToken(importToken);
        }
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void checkFeemaster(String feemasterServer)
    {
        disposable = alphaWalletService.checkFeemasterService(feemasterServer, network.getValue().chainId, importOrder.contractAddress)
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

    private List<Long> getNetworkIds()
    {
        List<Long> networkIds = new ArrayList<>();
        for (NetworkInfo networkInfo : ethereumNetworkRepository.getAvailableNetworkList())
        {
            networkIds.add(networkInfo.chainId);
        }
        return networkIds;
    }

    private void testNetworkResult(ContractLocator result)
    {
        if (!foundNetwork && !result.address.equals(TokenRepository.INVALID_CONTRACT))
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

    private void tryDefault(ContractLocator result, String method)
    {
        if (result.address.equals(TokenRepository.INVALID_CONTRACT))
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

    public void getAuthorisation(Activity activity, SignAuthenticationCallback callback)
    {
        if (wallet.getValue() != null)
        {
            keyService.getAuthenticationForSignature(wallet.getValue(), activity, callback);
        }
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public void checkTokenScriptSignature(final long chainId, final String address)
    {
        disposable = assetDefinitionService.getAssetDefinitionASync(chainId, address)
                .flatMap(def -> assetDefinitionService.getSignatureData(chainId, address))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(sig::postValue, this::onSigError);
    }

    private void onSigError(Throwable throwable)
    {
        XMLDsigDescriptor failSig = new XMLDsigDescriptor();
        failSig.result = "fail";
        failSig.type = SigReturnType.NO_TOKENSCRIPT;
        failSig.subject = throwable.getMessage();
        sig.postValue(failSig);
    }

    public void completeAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }

    public void failedAuthentication(Operation signData)
    {
        keyService.failedAuthentication(signData);
    }

    public void onDestroy()
    {
        if (getBalanceDisposable != null && !getBalanceDisposable.isDisposed()) getBalanceDisposable.dispose();
        getBalanceDisposable = null;
        gasService.stopGasPriceCycle();
    }
}