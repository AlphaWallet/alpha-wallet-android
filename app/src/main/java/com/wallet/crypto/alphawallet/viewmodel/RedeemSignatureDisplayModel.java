package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.Nullable;

import com.wallet.crypto.alphawallet.entity.MessagePair;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.SignaturePair;
import com.wallet.crypto.alphawallet.entity.SubscribeWrapper;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TransactionData;
import com.wallet.crypto.alphawallet.entity.TransactionInterpreter;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.MemPoolInteract;
import com.wallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;


import org.web3j.crypto.Hash;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import rx.functions.Action1;

/**
 * Created by James on 25/01/2018.
 */

public class RedeemSignatureDisplayModel extends BaseViewModel {
    private static final long CYCLE_SIGNATURE_INTERVAL = 30;
    private static final long CHECK_BALANCE_INTERVAL = 10;

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
    private TicketRange ticketRange;
    private List<Integer> ticketIndicies;

    @Nullable
    private Disposable getBalanceDisposable;

    @Nullable
    private Disposable cycleSignatureDisposable;

    private String address;
    //private BigInteger bitFieldLookup;
    private String lastSelection;
    private String newSelection;
    private int unchangedCount = 10;

    RedeemSignatureDisplayModel(
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

        if (wrapper != null && wrapper.wrapperInteraction != null) {
            wrapper.wrapperInteraction.sendEmptyMessage(1);
            wrapper = null;
        }
    }

    public void prepare(String address, Ticket ticket, TicketRange ticketRange) {
        this.address = address;
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
        this.ticketRange = ticketRange;
        this.ticket.setValue(ticket);
        this.ticketIndicies = ticket.ticketIdToTicketIndex(ticketRange.tokenIds);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void startCycleSignature() {
        cycleSignatureDisposable = Observable.interval(0, CYCLE_SIGNATURE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> signatureGenerateInteract
                        .getMessage(ticketIndicies)
                        .subscribe(this::onSignMessage, this::onError))
                .subscribe(l -> {}, t -> {});
    }

    private void startMemoryPoolListener() {
        wrapper = new SubscribeWrapper(scanReturn);
        memoryPoolInteract.poolListener(wrapper);
    }

    private void onSignMessage(MessagePair pair) {
        //now run this guy through the signed message system
        if (pair != null)
        disposable = createTransactionInteract
                .sign(defaultWallet.getValue(), pair)
                .subscribe(this::onSignedMessage, this::onError);
    }

    private void onSignedMessage(SignaturePair sigPair) {
        signature.postValue(sigPair);
    }

    public void newBalanceArray(String balanceArray) {
        newSelection = balanceArray;
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        startCycleSignature();
        //fetchTransactions();
        startMemoryPoolListener();

        //Push initial QR
        signatureGenerateInteract
                .getMessage(ticketIndicies)
                .subscribe(this::onSignMessage, this::onError);
    }

    private void markUsedIndicies(List<BigInteger> burnList) {
        Ticket t = ticket().getValue();
        for (BigInteger bi : burnList)
        {
            if (ticketIndicies.contains(bi.intValue()))
            {
                Integer index = bi.intValue();
                ticketIndicies.remove(index);
            }
        }
        //now write to burn indicies
        t.addToBurnList(burnList);

        updateBurnInfo(t.getBurnList());
    }

    private void updateBurnInfo(List<Integer> burnList)
    {
        fetchTokensInteract
                .updateBalance(defaultWallet().getValue(), ticket().getValue(), burnList)
                .subscribe(this::onSaved, this::onError);
    }

    private void onSaved()
    {
        //display 'burn complete'
        if (ticketIndicies.size() == 0)
        {
            signature.postValue(null);
        }
        else {
            signatureGenerateInteract
                    .getMessage(ticketIndicies)
                    .subscribe(this::onSignMessage, this::onError);
        }
    }

    //Handle each new transaction on memory pool
    private Action1<Transaction> scanReturn = (tx) ->
    {
        try
        {
            String input = tx.getInput();
            String from = Numeric.cleanHexPrefix(tx.getFrom());
            String to = Numeric.cleanHexPrefix(tx.getTo());
            String userAddr = Numeric.cleanHexPrefix(defaultWallet().getValue().address);

            String methodSignature = "transferFrom(address,address,uint16[])";
            String methodId = buildMethodId(methodSignature);

            if (       (from != null)
                    && (to != null && ticket.getValue().tokenInfo.address.contains(to))
                    && (input != null)
                    && (input.contains(methodId))
                    && (input.contains("dead") && input.contains(userAddr))  )
            {
                System.out.println("Burn Transaction!");
                TransactionInterpreter t = new TransactionInterpreter();

                TransactionData data = t.InterpretTransation(input);

                if (data.functionData.functionName.equals("transferFrom"))
                {
                    //pass the list of args back into token listing
                    List<BigInteger> transferIndicies = data.paramValues;
                    markUsedIndicies(transferIndicies);
                }
            }
        }
        catch (Exception e)
        {
            //e.printStackTrace();
        }
    };

    private String buildMethodId(String methodSignature) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = Hash.sha3(input);
        return Numeric.toHexString(hash).substring(0, 10);
    }
}
