package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;

import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;
import io.awallet.crypto.alphawallet.entity.ServiceErrorException;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModel extends BaseViewModel  {
    private static final long CHECK_BALANCE_INTERVAL = 10;

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;

    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<TicketRange> importRange = new MutableLiveData<>();
    private final MutableLiveData<Integer> invalidRange = new MutableLiveData<>();
    private final MutableLiveData<Boolean> invalidLink = new MutableLiveData<>();

    private SalesOrder importOrder;
    private String univeralImportLink;
    private String ownerAddress;
    private Ticket importToken;
    private List<Integer> availableBalance = new ArrayList<>();
    private Map<String, Token> tokenMap = new HashMap<>();
    private double priceUsd;
    private double ethToUsd;

    @Nullable
    private Disposable getBalanceDisposable;

    ImportTokenViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                         CreateTransactionInteract createTransactionInteract,
                         FetchTokensInteract fetchTokensInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
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

    private void onWallet(Wallet wallet) {
        this.wallet.setValue(wallet);
        try {
            importOrder = SalesOrder.parseUniversalLink(univeralImportLink);
            //ecrecover the owner
            byte[] message = importOrder.message;
            Sign.SignatureData sigData;
            sigData = sigFromByteArray(importOrder.signature);
            BigInteger recoveredKey = Sign.signedMessageToKey(message, sigData);
            ownerAddress = "0x" + Keys.getAddress(recoveredKey);
            //start looking at the ticket details
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

    private void fetchTokens() {
        getBalanceDisposable = Observable.interval(0, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetchList(new Wallet(ownerAddress))
                        .subscribe(this::onTokens)).subscribe();
    }

    private void onTokens(Map<String, Token> tokenMap)
    {
        //check the required balance
        if (tokenMap.get(wallet().getValue().address).ticker != null) {
            ethToUsd = Double.valueOf(tokenMap.get(wallet().getValue().address).ticker.price);
        }
        Token contractToken = tokenMap.get(importOrder.contractAddress);
        if (contractToken != null) {
            importToken = (Ticket) contractToken;
            updateToken();
        }
    }

    private void updateToken()
    {
        List<Integer> newBalance = new ArrayList<>();
        //calculate USD price of tickets
        priceUsd = importOrder.price * ethToUsd;
        for (Integer index : importOrder.tickets) //SalesOrder tickets member contains the list of ticket indices we're importing
        {
            if (importToken.balanceArray.size() > index) {
                Integer ticketId = importToken.balanceArray.get(index);
                if (ticketId > 0) {
                    newBalance.add(ticketId);
                }
            }
        }

        if (newBalance.size() > 0 && balanceChange(newBalance)) {
            availableBalance = newBalance;
            TicketRange range = new TicketRange(availableBalance.get(0), importToken.getAddress());
            for (int i = 1; i < availableBalance.size(); i++)
            {
                range.tokenIds.add(availableBalance.get(i));
            }
            importRange.setValue(range);
        }
        else if (newBalance.size() == 0)
        {
            //tickets already imported
            invalidRange.setValue(importOrder.ticketCount);
        }
    }

    private boolean balanceChange(List<Integer> newBalance)
    {
        return !(newBalance.containsAll(availableBalance) && availableBalance.containsAll(newBalance));
    }

    public void onError(Throwable throwable) {
        if (throwable.getCause() instanceof ServiceErrorException) {
            if (((ServiceErrorException) throwable.getCause()).code == C.ErrorCode.ALREADY_ADDED){
                error.postValue(new ErrorEnvelope(C.ErrorCode.ALREADY_ADDED, null));
            }
        } else {
            error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
        }
    }

    //TODO: Confirm purchase if required
    public void performImport() {
        try {
            SalesOrder order = SalesOrder.parseUniversalLink(univeralImportLink);
            //ok let's try to drive this guy through
            final byte[] tradeData = SalesOrder.generateReverseTradeData(order);
            System.out.println("Approx value of trade: " + order.price);
            //now push the transaction
            disposable = createTransactionInteract
                    .create(wallet.getValue(), order.contractAddress, order.priceWei,
                            Contract.GAS_PRICE, Contract.GAS_LIMIT, tradeData)
                    .subscribe(this::onCreateTransaction, this::onError);
        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace(); // TODO: add user interface handling of the exception.
        }
    }

    private void onCreateTransaction(String transaction)
    {
        newTransaction.postValue(transaction);
    }
}