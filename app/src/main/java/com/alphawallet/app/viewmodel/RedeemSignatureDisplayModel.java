package com.alphawallet.app.viewmodel;

import android.app.Activity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.os.NetworkOnMainThreadException;
import androidx.annotation.Nullable;

import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.tokens.Ticket;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.TransferFromEventResponse;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.MemPoolInteract;
import com.alphawallet.app.interact.SignatureGenerateInteract;

import com.alphawallet.app.service.KeyService;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.service.TokensService;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.service.AssetDefinitionService;

import javax.inject.Inject;

/**
 * Created by James on 25/01/2018.
 */
@HiltViewModel
public class RedeemSignatureDisplayModel extends BaseViewModel
{
    private static final long CYCLE_SIGNATURE_INTERVAL = 10 * 60; //cycle every 10 minutes
    private static final long CHECK_BALANCE_INTERVAL = 10;

    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final MemPoolInteract memoryPoolInteract;
    private final TokensService tokensService;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<SignaturePair> signature = new MutableLiveData<>();

    private final MutableLiveData<String> selection = new MutableLiveData<>();
    private final MutableLiveData<Boolean> burnNotice = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signRequest = new MutableLiveData<>();

    private Disposable memPoolSubscription;
    private final List<BigInteger> tickets = new ArrayList<>();
    private Token token;

    @Nullable
    private Disposable getBalanceDisposable;

    @Nullable
    private Disposable cycleSignatureDisposable;

    private String address;

    @Inject
    RedeemSignatureDisplayModel(
            GenericWalletInteract genericWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            CreateTransactionInteract createTransactionInteract,
            KeyService keyService,
            FetchTokensInteract fetchTokensInteract,
            MemPoolInteract memoryPoolInteract,
            TokensService tokensService,
            AssetDefinitionService assetDefinitionService) {
        this.genericWalletInteract = genericWalletInteract;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.memoryPoolInteract = memoryPoolInteract;
        this.tokensService = tokensService;
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

    public TokensService getTokensService() { return tokensService; }

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

    private void fetchTokenBalance() {
        progress.postValue(true);
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .updateDefaultBalance(token, defaultWallet.getValue())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onToken, this::onError)).subscribe();
    }

    private void checkRedeemTicket() {
        progress.postValue(true);
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .checkRedeemed(token, this.tickets)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onRedeemCheck, this::onError)).subscribe();
    }

    private void onRedeemCheck(Boolean redeemed)
    {
        if (redeemed)
        {
            ticketsBurned();
        }
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
                for (BigInteger index : this.tickets)
                {
                    if (token.isERC721Ticket() && !index.equals(BigInteger.ZERO)) //handle ERC721
                    {
                        allBurned = false;
                        break;
                    }
                    else if (!balance.get(index.intValue()).equals(BigInteger.ZERO))
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

        tickets.clear();
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
        tickets.clear();
        if(ticket instanceof Ticket)
        {
            this.tickets.addAll(((Ticket)ticket).ticketIdListToIndexList(ticketRange.tokenIds));
        }
        else
        {
            this.tickets.addAll(ticketRange.tokenIds);
        }
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
                .getMessage(tickets, token.getAddress(), this.token.getInterfaceSpec())
                .subscribe(pair -> onSignMessage(pair, wallet), this::onError)
                .isDisposed();
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
        if (token.isERC721Ticket())
        {
            checkRedeemTicket();
        }
        else
        {
            fetchTokenBalance();
        }
        startMemoryPoolListener();

        onSaved();
    }

    private void markUsedIndicies(List<Uint16> burnList) {
        for (Uint16 indexVal : burnList)
        {
            Integer index = indexVal.getValue().intValue();
            tickets.remove(index);
        }
    }

    private void onSaved()
    {
        //display 'burn complete'
        if (tickets.size() == 0)
        {
            ticketsBurned();
        }
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

    public void completeAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }
}
