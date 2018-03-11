package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.ServiceErrorException;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.ImportWalletInteract;
import com.wallet.crypto.alphawallet.service.ImportTokenService;
import com.wallet.crypto.alphawallet.ui.widget.OnImportKeystoreListener;
import com.wallet.crypto.alphawallet.ui.widget.OnImportPrivateKeyListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.wallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

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

    private SalesOrder importOrder;
    private String univeralImportLink;
    private String ownerAddress;
    private Ticket importToken;
    private List<Integer> availableBalance = new ArrayList<>();

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

    private void onWallet(Wallet wallet) {
        this.wallet.setValue(wallet);
        importOrder = SalesOrder.parseUniversalLink(univeralImportLink);

        //ecrecover the owner
        byte[] message = importOrder.message;
        Sign.SignatureData sigData;
        try {
            sigData = sigFromByteArray(importOrder.signature);
            BigInteger recoveredKey = Sign.signedMessageToKey(message, sigData);
            ownerAddress = "0x" + Keys.getAddress(recoveredKey);
            //start looking at the ticket details
            fetchBalance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //now get balance at recovered address
        //1. add required interact
        //2. after we get balance, check these tokens are still at that address, get the ID's and update the import token
        //3. allow import to continue.
    }

    public void fetchBalance() {
        getBalanceDisposable = Observable.interval(0, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetch(new Wallet(ownerAddress))
                        .subscribe(this::onBalance)).subscribe();
    }

    private void onBalance(Token[] tokens)
    {
        //check the required balance
        for (Token t : tokens)
        {
            if (t.addressMatches(importOrder.contractAddress) && t instanceof Ticket)
            {
                importToken = (Ticket)t;
                updateToken();
                break;
            }
        }
    }

    private void updateToken()
    {
        List<Integer> newBalance = new ArrayList<>();
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
        if (newBalance.containsAll(availableBalance) && availableBalance.containsAll(newBalance))
        {
            return false;
        }
        else
        {
            return true;
        }
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

    public void performImport() {
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

    private void onCreateTransaction(String transaction)
    {
        newTransaction.postValue(transaction);
    }
}
