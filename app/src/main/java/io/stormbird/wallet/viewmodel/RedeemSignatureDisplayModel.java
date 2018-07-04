package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.Nullable;

import io.stormbird.wallet.entity.MessagePair;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.SignaturePair;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TransferFromEventResponse;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.MemPoolInteract;
import io.stormbird.wallet.interact.SignatureGenerateInteract;
import io.stormbird.wallet.router.AssetDisplayRouter;


import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 25/01/2018.
 */

public class RedeemSignatureDisplayModel extends BaseViewModel
{
    private static final long CYCLE_SIGNATURE_INTERVAL = 30;
    private static final long CHECK_BALANCE_INTERVAL = 10;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final MemPoolInteract memoryPoolInteract;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<SignaturePair> signature = new MutableLiveData<>();

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Ticket> ticket = new MutableLiveData<>();

    private final MutableLiveData<String> selection = new MutableLiveData<>();
    private final MutableLiveData<Boolean> burnNotice = new MutableLiveData<>();

    private rx.Subscription memPoolSubscription;
    private List<Integer> ticketIndicies;

    @Nullable
    private Disposable getBalanceDisposable;

    @Nullable
    private Disposable cycleSignatureDisposable;

    private String address;

    RedeemSignatureDisplayModel(
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            CreateTransactionInteract createTransactionInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FetchTokensInteract fetchTokensInteract,
            MemPoolInteract memoryPoolInteract,
            AssetDisplayRouter assetDisplayRouter,
            AssetDefinitionService assetDefinitionService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.memoryPoolInteract = memoryPoolInteract;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
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
    public LiveData<Boolean> burnNotice() {
        return burnNotice;
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
        if (!memPoolSubscription.isUnsubscribed()) {
            closeListener()
                    .subscribeOn(Schedulers.newThread())
                    .subscribe();
        }
    }

    public void fetchTokenBalance() {
        progress.postValue(true);
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetch(defaultWallet.getValue())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onTokens)).subscribe();
    }

    /**
     * This is a fallback check - it polls balance. If we somehow miss the memory pool pickup
     * then this method will pick up when the transaction is written to blockchain
     */
    private void onTokens(Token[] tokens) {
        this.tokens.setValue(tokens);

        for (Token t : tokens) {
            if (t instanceof Ticket && t.tokenInfo.address.equals(address))
            {
                boolean allBurned = true;
                //See if our tickets got burned
                for (Integer index : this.ticketIndicies)
                {
                    if (!((Ticket) t).balanceArray.get(index).equals(BigInteger.ZERO))
                    {
                        allBurned = false;
                        break;
                    }
                }

                if (allBurned)
                {
                    ticketsBurned();
                }
                break;
            }
        }
    }

    private void ticketsBurned()
    {
        if (cycleSignatureDisposable != null) {
            cycleSignatureDisposable.dispose();
        }
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        if (!memPoolSubscription.isUnsubscribed()) {
            closeListener()
                    .subscribeOn(Schedulers.newThread())
                    .subscribe();
        }

        ticketIndicies.clear();
        burnNotice.postValue(true);
    }

    private Single<Boolean> closeListener()
    {
        return Single.fromCallable(() -> {
            try {
                memPoolSubscription.unsubscribe();
                return true;
            } catch (NetworkOnMainThreadException th) {
                // Ignore all errors, it's not important source.
                return false;
            }
        });
    }

    public void prepare(String address, Ticket ticket, TicketRange ticketRange) {
        this.address = address;
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
        this.ticket.setValue(ticket);
        this.ticketIndicies = ticket.ticketIdListToIndexList(ticketRange.tokenIds);
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
                        .getMessage(ticketIndicies, this.ticket.getValue().getAddress())
                        .subscribe(this::onSignMessage, this::onError))
                .subscribe(l -> {}, t -> {});
    }

    private void startMemoryPoolListener() {
        memPoolSubscription = memoryPoolInteract.burnListener(ticket.getValue().getAddress())
                .subscribeOn(rx.schedulers.Schedulers.newThread())
                .subscribe(this::receiveBurnNotification, this::onBurnError);
    }

    private void receiveBurnNotification(TransferFromEventResponse burnTx)
    {
        String userAddr = Numeric.cleanHexPrefix(defaultWallet().getValue().address).toLowerCase();

        if (burnTx._from.toLowerCase().contains(userAddr))
        {
            List<Uint16> transferIndicies = burnTx._indices;
            markUsedIndicies(transferIndicies);
        }
    }

    //restart the listener - sometimes blockchain throws a wobbly
    private void onBurnError(Throwable throwable)
    {
        if (!memPoolSubscription.isUnsubscribed()) memPoolSubscription.unsubscribe();
        startMemoryPoolListener();
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

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        startCycleSignature();
        fetchTokenBalance();
        startMemoryPoolListener();

        onSaved();
    }

    private void markUsedIndicies(List<Uint16> burnList) {
        Ticket t = ticket().getValue();
        for (Uint16 indexVal : burnList)
        {
            Integer index = indexVal.getValue().intValue();
            if (ticketIndicies.contains(index))
            {
                ticketIndicies.remove(index);
            }
        }
        //now write to burn indicies
        t.addToBurnList(burnList);
        updateBurnInfo(t.getBurnList());
    }

    private void updateBurnInfo(List<Integer> burnList)
    {
        disposable = fetchTokensInteract
                .updateBalance(defaultWallet().getValue(), ticket().getValue(), burnList)
                .subscribe(this::onSaved, this::onError);
    }

    private void onSaved()
    {
        //display 'burn complete'
        if (ticketIndicies.size() == 0)
        {
            ticketsBurned();
        }
        else {
            disposable = signatureGenerateInteract
                    .getMessage(ticketIndicies, this.ticket.getValue().getAddress())
                    .subscribe(this::onSignMessage, this::onError);
        }
    }

    public void showAssets(Context context, Ticket t, boolean isClearStack) {
        assetDisplayRouter.open(context, t, isClearStack);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
