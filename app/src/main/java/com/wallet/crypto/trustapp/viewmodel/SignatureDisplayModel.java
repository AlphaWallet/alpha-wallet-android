package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.wallet.crypto.trustapp.entity.MessagePair;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.SignaturePair;
import com.wallet.crypto.trustapp.entity.SubscribeWrapper;
import com.wallet.crypto.trustapp.entity.Ticket;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.CreateTransactionInteract;
import com.wallet.crypto.trustapp.interact.FetchTokensInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.MemPoolInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;


import org.web3j.protocol.core.methods.response.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.Provides;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by James on 25/01/2018.
 */

public class SignatureDisplayModel extends BaseViewModel {
    private static final long CYCLE_SIGNATURE_INTERVAL = 30;
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final long CHECK_SELECTION_INTERVAL = 1;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final MemPoolInteract memoryPoolInteract;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<SignaturePair> signature = new MutableLiveData<>();

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Ticket> ticket = new MutableLiveData<>();

    private final MutableLiveData<String> selection = new MutableLiveData<>();

    private SubscribeWrapper wrapper;

    private Subscription poolListener;

    @Nullable
    private Disposable getBalanceDisposable;

    @Nullable
    private Disposable cycleSignatureDisposable;

    @Nullable
    private Disposable checkSelectionDisposable;

    private String address;
    private BigInteger bitFieldLookup;
    private String lastSelection;
    private String newSelection;
    private int unchangedCount = 10;

    SignatureDisplayModel(
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            CreateTransactionInteract createTransactionInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FetchTokensInteract fetchTokensInteract,
            MemPoolInteract memoryPoolInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.memoryPoolInteract = memoryPoolInteract;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<SignaturePair> signature() {
        return signature;
    }
    public LiveData<Ticket> ticket() {
        return ticket;
    }
    public LiveData<String> selection() {
        return selection;
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (cycleSignatureDisposable != null) {
            cycleSignatureDisposable.dispose();
        }
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        if (checkSelectionDisposable != null) {
            checkSelectionDisposable.dispose();
        }

        if (wrapper != null && wrapper.transactionSubscriber != null) {
            wrapper.transactionSubscriber.unsubscribe();
        }
    }

    public void prepare(String address) {
        this.address = address;
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void fetchTransactions() {
        progress.postValue(true);
        getBalanceDisposable = Observable.interval(0, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetch(defaultWallet.getValue())
                        .subscribe(this::onTokens, t -> {}))
                .subscribe(l -> {}, t -> {});
    }

    //TODO: Modulate the wallet message
    //TODO: Collect all the IDs to be sent, encode them into QR code
    private void startCycleSignature() {
        cycleSignatureDisposable = Observable.interval(0, CYCLE_SIGNATURE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> signatureGenerateInteract
                        .getMessage(bitFieldLookup)
                        .subscribe(this::onSignMessage, this::onError))
                .subscribe(l -> {}, t -> {});
    }

    private void startMemoryPoolListener() {
        wrapper = new SubscribeWrapper(scanReturn);
        memoryPoolInteract.poolListener(wrapper);
    }

    private void onSignMessage(MessagePair pair) {
        //now run this guy through the signed message system
        disposable = createTransactionInteract
                .sign(defaultWallet.getValue(), pair)
                .subscribe(this::onSignedMessage, this::onError);
    }

    private void onSignedMessage(SignaturePair sigPair) {
        signature.postValue(sigPair);
    }

    private void startSelectionCheck() {
        checkSelectionDisposable = Observable.interval(0, CHECK_SELECTION_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> checkSelectionChanged())
                .subscribe(l -> {}, t -> {});
    }

    private void checkSelectionChanged()
    {
        unchangedCount++;

        if (unchangedCount == 2)
        {
            //do the new selection
            changeSelection();

            //push to QR
            signatureGenerateInteract
                    .getMessage(bitFieldLookup)
                    .subscribe(this::onSignMessage, this::onError);
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
        try
        {
            List<Integer> indexList = ticket.getValue().parseIndexList(newSelection);
            //convert this to a bitfield
            if (indexList != null && indexList.size() > 0)
            {
                bitFieldLookup = BigInteger.ZERO;
                for (Integer i : indexList)
                {
                    BigInteger adder = BigInteger.valueOf(2).pow(i);
                    bitFieldLookup = bitFieldLookup.add(adder);
                }

                //now convert back to parsed string, with checked indicies
                String neatSelection = ticket.getValue().parseList(indexList);
                selection.postValue(neatSelection);

                //String hexVal = Integer.toHexString(bitFieldLookup.intValue());
            }
            else
            {
                selection.postValue("");
                bitFieldLookup = BigInteger.ZERO;
            }
        }
        catch (Exception e)
        {

        }
    }

    public void newBalanceArray(String balanceArray) {
        newSelection = balanceArray;
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        startCycleSignature();
        fetchTransactions();
        startSelectionCheck();
        startMemoryPoolListener();
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

    //Handle each new transaction on memory pool
    private Action1<Transaction> scanReturn = (tx) ->
    {
        try
        {
            String input = tx.getInput();
            String from = tx.getFrom();
            String to = tx.getTo();
            String userAddr = defaultWallet().getValue().address;

            if ((from != null
                    && (to != null)
                    && (input != null && input.contains("dead"))))
            {
                userAddr = input.substring(34, 34 + 40);
                System.out.println("TX: input " + input);
                System.out.println("TX: user " + userAddr);
                System.out.println("TX: from " + from);
                System.out.println("TX: to" + to);

            }
        }
        catch (Exception e)
        {

        }
    };
}
