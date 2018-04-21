package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;
import android.util.Log;

import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;
import io.awallet.crypto.alphawallet.entity.ServiceErrorException;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenFactory;
import io.awallet.crypto.alphawallet.entity.TokenInfo;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.SetupTokensInteract;
import io.awallet.crypto.alphawallet.service.FeeMasterService;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static io.awallet.crypto.alphawallet.C.ETH_SYMBOL;
import static io.awallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModel extends BaseViewModel  {
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final String TAG = "ITVM";

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final FeeMasterService feeMasterService;

    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<TicketRange> importRange = new MutableLiveData<>();
    private final MutableLiveData<Integer> invalidRange = new MutableLiveData<>();
    private final MutableLiveData<Boolean> invalidLink = new MutableLiveData<>();

    private SalesOrder importOrder;
    private String univeralImportLink;
    private Ticket importToken;
    private List<BigInteger> availableBalance = new ArrayList<>();
    private double priceUsd;
    private double ethToUsd = 0;

    @Nullable
    private Disposable getBalanceDisposable;

    ImportTokenViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                         CreateTransactionInteract createTransactionInteract,
                         FetchTokensInteract fetchTokensInteract,
                         SetupTokensInteract setupTokensInteract,
                         FeeMasterService feeMasterService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.feeMasterService = feeMasterService;
    }

    public LiveData<TicketRange> importRange() {
        return importRange;
    }
    public LiveData<Integer> invalidRange() { return invalidRange; }
    public LiveData<String> newTransaction() { return newTransaction; }
    public LiveData<Boolean> invalidLink() { return invalidLink; }
    public double getUSDPrice() { return priceUsd; };

    public void prepare(String importDataStr) {
        univeralImportLink = importDataStr;
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onWallet, this::onError);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
    }

    public LiveData<Wallet> wallet() {
        return wallet;
    }
    public Ticket getImportToken() { return importToken; }
    public SalesOrder getSalesOrder() { return importOrder; }

    //1. Receive the default wallet (if any), then decode the import order
    private void onWallet(Wallet wallet) {
        this.wallet.setValue(wallet);
        try {
            importOrder = SalesOrder.parseUniversalLink(univeralImportLink);
            //ecrecover the owner
            importOrder.getOwnerKey();
            //got to step 2. - get cached tokens
            fetchTokens();
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

    //2. Fetch all cached tokens and get eth price
    private void fetchTokens() {
        importToken = null;
        disposable = fetchTokensInteract
                .fetchStored(new Wallet(importOrder.ownerAddress))
                .subscribe(this::onTokens, this::onError, this::fetchTokensComplete);
    }

    //2a. receive a stream of tokens.
    //- store eth price from ticker
    //- get the token corresponding to the import order if we already cached it
    //TODO: Optimise, feed one token at a time, and only use cached tokens. Seperatately fetch the ticker after the token has been checked
    private void onTokens(Token[] tokens) {
        for (Token token : tokens)
        {
            if (token.ticker != null && token.tokenInfo.symbol.equals(ETH_SYMBOL))
            {
                ethToUsd = Double.valueOf(token.ticker.price);
            }
            else if (token.addressMatches(importOrder.contractAddress) && token instanceof Ticket)
            {
                importToken = (Ticket) token;
            }
        }
    }

    //2b. on completion of receiving tokens check if we found the matching token
    private void fetchTokensComplete()
    {
        if (importToken == null)
        {
            //Didn't have the token cached, so retrieve it from
            setupTokenAddr(importOrder.contractAddress);
        }
        else
        {
            updateToken();
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
            Token tempToken = tf.createToken(tokenInfo);

            disposable = fetchTokensInteract.updateBalance(importOrder.ownerAddress, tempToken)
                    .subscribeOn(Schedulers.io()) //observeOn
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onBalance, this::onError, this::gotBalance);
        }
        else
        {
            invalidLink.postValue(true);
        }
    }

    //4a. Receive balance
    private void onBalance(Token token)
    {
        if (token != null && token instanceof Ticket) {
            importToken = (Ticket) token;
        }

        //normBalance(token);
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

//        getBalanceDisposable = fetchTokensInteract
//                        .updateBalance("0x93922cdabaa26d50e7c6cb19ee3bcd03462ed334", importToken)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(this::extraBalance);
    }

//    private void normBalance(Token token)
//    {
//        Ticket tt = (Ticket) token;
//        String first5 = importOrder.ownerAddress.substring(0, 5);
//        for (BigInteger bi : tt.balanceArray)
//        {
//            Log.d(TAG, first5 + ": " + bi.toString(16));
//        }
//    }

//    private void extraBalance(Token token)
//    {
//        Ticket tt = (Ticket) token;
//        for (BigInteger bi : tt.balanceArray)
//        {
//            Log.d(TAG, "0x939: " + bi.toString(16));
//        }
//    }

    //5. We have token information and balance. Check if the import order is still valid.
    private void updateToken()
    {
        List<BigInteger> newBalance = new ArrayList<>();
        //calculate USD price of tickets
        priceUsd = importOrder.price * ethToUsd;

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

        if (newBalance.size() == 0 || newBalance.size() != importOrder.tickets.length)
        {
            //tickets already imported
            invalidRange.setValue(newBalance.size());
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

    //perform a balance check cycle every CHECK_BALANCE_INTERVAL seconds
    private void regularBalanceCheck()
    {
        getBalanceDisposable = Observable.interval(0, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .updateBalance(importOrder.ownerAddress, importToken)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onBalance, this::onError, this::gotBalance)).subscribe();
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
        if (throwable.getCause() instanceof ServiceErrorException) {
            if (((ServiceErrorException) throwable.getCause()).code == C.ErrorCode.ALREADY_ADDED){
                error.postValue(new ErrorEnvelope(C.ErrorCode.ALREADY_ADDED, null));
            }
        } else {
            error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
        }
    }

    public void performImport()
    {
        try
        {
            SalesOrder order = SalesOrder.parseUniversalLink(univeralImportLink);
            //ok let's try to drive this guy through
            final byte[] tradeData = SalesOrder.generateReverseTradeData(order);
            Log.d(TAG, "Approx value of trade: " + order.price);
            //now push the transaction
            disposable = createTransactionInteract
                    .create(wallet.getValue(), order.contractAddress, order.priceWei,
                            Contract.GAS_PRICE, Contract.GAS_LIMIT, tradeData)
                    .subscribe(this::onCreateTransaction, this::onError);
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
            SalesOrder order = SalesOrder.parseUniversalLink(univeralImportLink);
            disposable = feeMasterService.handleFeemasterImport(url, wallet.getValue(), order)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::processFeemasterResult, this::onError);
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
}