package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketInfo;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;

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

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Ticket> ticket = new MutableLiveData<>();

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;

    private String address;

    public MarketOrderViewModel(
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
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

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
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
    }

    private void onFetchTokensCompletable() {
        progress.postValue(false);
        Token[] tokens = tokens().getValue();
        if (tokens == null || tokens.length == 0) {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
        fetchTransactions();
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
}
