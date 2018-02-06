package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketInfo;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 5/02/2018.
 */

public class MarketOrderViewModel extends BaseViewModel
{
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final long CHECK_SELECTION_INTERVAL = 1;

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Ticket> ticket = new MutableLiveData<>();

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    private final MutableLiveData<String> selection = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable checkSelectionDisposable;

    private String address;
    private int unchangedCount = 10;
    private String lastSelection;
    private String newSelection;

    public MarketOrderViewModel(
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            CreateTransactionInteract createTransactionInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.createTransactionInteract = createTransactionInteract;
    }

    public void prepare(String address) {
        this.address = address;
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void fetchTransactions() {
        progress.postValue(true);
        getBalanceDisposable = Observable.interval(0, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetch(defaultWallet.getValue())
                        .subscribe(this::onTokens, t -> {}))
                .subscribe(l -> {}, t -> {});
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }
    public LiveData<Ticket> ticket() {
        return ticket;
    }
    public LiveData<String> selection() {
        return selection;
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void startSelectionCheck() {
        checkSelectionDisposable = Observable.interval(0, CHECK_SELECTION_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> checkSelectionChanged())
                .subscribe(l -> {}, t -> {});
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        disposable = fetchTokensInteract
                .fetch(wallet)
                .subscribe(this::onTokens, this::onError, this::onFetchTokensCompletable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        if (checkSelectionDisposable != null) {
            checkSelectionDisposable.dispose();
        }
    }

    private void onFetchTokensCompletable() {
        progress.postValue(false);
        Token[] tokens = tokens().getValue();
        if (tokens == null || tokens.length == 0) {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
        fetchTransactions();
        startSelectionCheck();
    }

    private void onTokens(Token[] tokens) {
        if (tokens != null && tokens.length > 0) {
            progress.postValue(true);
        }
        this.tokens.setValue(tokens);

        for (Token t : tokens) {
            if (t instanceof Ticket && t.tokenInfo.address.equals(address))
            {
                ticket.setValue((Ticket)t);
                ticket.postValue((Ticket)t);
                break;
            }
        }
    }

    private void checkSelectionChanged()
    {
        unchangedCount++;

        if (unchangedCount == 2)
        {
            //do the new selection
            changeSelection();
        }

        if (newSelection != null && !newSelection.equals(lastSelection))
        {
            lastSelection = newSelection;
            unchangedCount = 0;
        }
    }

    private void changeSelection()
    {
        //convert to array of indicies
        try {
            List<Integer> indexList = ticket.getValue().parseIndexList(newSelection);
            //convert this to a bitfield
            if (indexList != null && indexList.size() > 0) {
                String neatSelection = ticket.getValue().parseList(indexList);
                selection.postValue(neatSelection);
            } else {
                selection.postValue("");
            }
        } catch (Exception e) {

        }
    }

    public void newBalanceArray(String balanceArray) {
        newSelection = balanceArray;
    }

    public void generateNewSelection(String selection) {
        newSelection = selection;
        //do the new selection
        changeSelection();
    }

    public void generateMarketOrders(List<Integer> idSendList)
    {
        short[] ticketIDs = new short[idSendList.size()];
        int index = 0;
        for (Integer i : idSendList) {
            ticketIDs[index++] = i.shortValue();
        }

        BigInteger price = BigInteger.TEN;

        //Use base queue otherwise the queue g
        createTransactionInteract.createMarketOrders(defaultWallet.getValue(), price, ticketIDs, ticket().getValue(), this::onCompleteMarketTask, this::onError, this::onAllTransactions);

        System.out.println("go");
    }

    public void onOrdersCreated(TradeInstance[] trades)
    {
        for (TradeInstance t : trades) {
            System.out.println("Expiry: " + t.getExpiryString() + " Order Sig: " + t.getStringSig());
        }
    }
}
