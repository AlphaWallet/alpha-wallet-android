package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.Nullable;

import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.Ticket;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TransferFromEventResponse;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.MemPoolInteract;
import com.alphawallet.app.interact.SignatureGenerateInteract;
import com.alphawallet.app.router.AssetDisplayRouter;

import com.alphawallet.app.service.KeyService;
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
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by James on 25/01/2018.
 */

public class RedeemSignatureDisplayModel extends BaseViewModel
{
    private static final long CYCLE_SIGNATURE_INTERVAL = 30;
    private static final long CHECK_BALANCE_INTERVAL = 10;

    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final MemPoolInteract memoryPoolInteract;
    private final AssetDisplayRouter assetDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<SignaturePair> signature = new MutableLiveData<>();

    private final MutableLiveData<String> selection = new MutableLiveData<>();
    private final MutableLiveData<Boolean> burnNotice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signRequest = new MutableLiveData<>();

    private Disposable memPoolSubscription;
    private List<Integer> ticketIndicies;
    private Token token;

    @Nullable
    private Disposable getBalanceDisposable;

    @Nullable
    private Disposable cycleSignatureDisposable;

    private String address;

    RedeemSignatureDisplayModel(
            GenericWalletInteract genericWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            CreateTransactionInteract createTransactionInteract,
            KeyService keyService,
            FetchTokensInteract fetchTokensInteract,
            MemPoolInteract memoryPoolInteract,
            AssetDisplayRouter assetDisplayRouter,
            AssetDefinitionService assetDefinitionService) {
        this.genericWalletInteract = genericWalletInteract;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.keyService = keyService;
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
    public LiveData<String> selection() {
        return selection;
    }
    public LiveData<Boolean> burnNotice() {
        return burnNotice;
    }
    public LiveData<Boolean> signRequest() {
        return signRequest;
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
        if (memPoolSubscription != null && !memPoolSubscription.isDisposed()) {
            closeListener()
                    .subscribeOn(Schedulers.newThread())
                    .subscribe();
        }
    }

    public void fetchTokenBalance() {
        progress.postValue(true);
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .updateDefaultBalance(token, defaultWallet.getValue())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onToken, this::onError)).subscribe();
    }

    /**
     * This is a fallback check - it polls balance. If we somehow miss the memory pool pickup
     * then this method will pick up when the transaction is written to blockchain
     */
    private void onToken(Token token)
    {
        this.token = token;

        if (token != null && token.tokenInfo.address.equals(address) && token.hasArrayBalance())
        {
            boolean allBurned = true;
            List<BigInteger> balance = token.getArrayBalance();
            //See if our indices got burned
            if (balance != null)
            {
                for (Integer index : this.ticketIndicies)
                {
                    if (!balance.get(index).equals(BigInteger.ZERO))
                    {
                        allBurned = false;
                        break;
                    }
                }
            }

            if (allBurned)
            {
                ticketsBurned();
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
        if (!memPoolSubscription.isDisposed()) {
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
                memPoolSubscription.dispose();
                return true;
            } catch (NetworkOnMainThreadException th) {
                // Ignore all errors, it's not important source.
                return false;
            }
        });
    }

    public void prepare(String address, Token ticket, TicketRange ticketRange) {
        this.address = address;
        token = ticket;
        this.ticketIndicies = ((Ticket)ticket).ticketIdListToIndexList(ticketRange.tokenIds);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void startCycleSignature() {
        cycleSignatureDisposable = Observable.interval(0, CYCLE_SIGNATURE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> signRequest.postValue(true))
                .subscribe(l -> {}, t -> {});
    }

    public void updateSignature(Wallet wallet)
    {
        signatureGenerateInteract
                .getMessage(ticketIndicies, token.getAddress())
                .subscribe(pair -> onSignMessage(pair, wallet), this::onError);
    }

    private void startMemoryPoolListener() {
        memPoolSubscription = memoryPoolInteract.burnListener(token.getAddress())
                .subscribeOn(Schedulers.newThread())
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
        if (!memPoolSubscription.isDisposed()) memPoolSubscription.dispose();
        startMemoryPoolListener();
    }

    private void onSignMessage(MessagePair pair, Wallet wallet) {
        //now run this guy through the signed message system
        if (pair != null)
        disposable = createTransactionInteract
                .sign(wallet, pair, token.tokenInfo.chainId)
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
        for (Uint16 indexVal : burnList)
        {
            Integer index = indexVal.getValue().intValue();
            if (ticketIndicies.contains(index))
            {
                ticketIndicies.remove(index);
            }
        }
    }

    private void onSaved()
    {
        //display 'burn complete'
        if (ticketIndicies.size() == 0)
        {
            ticketsBurned();
        }
    }

    public void showAssets(Context context, Ticket t, boolean isClearStack) {
        assetDisplayRouter.open(context, t, isClearStack);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void getAuthorisation(Activity activity, SignAuthenticationCallback callback)
    {
        if (defaultWallet.getValue() != null)
        {
            keyService.getAuthenticationForSignature(defaultWallet.getValue(), activity, callback);
        }
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }
}
