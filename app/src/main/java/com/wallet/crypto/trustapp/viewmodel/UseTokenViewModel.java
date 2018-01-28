package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.AddTokenInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SetupTokensInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;
import com.wallet.crypto.trustapp.interact.UseTokenInteract;
import com.wallet.crypto.trustapp.router.MyTokensRouter;
import com.wallet.crypto.trustapp.router.SendTokenRouter;
import com.wallet.crypto.trustapp.router.SignatureDisplayRouter;
import com.wallet.crypto.trustapp.router.TicketTransferRouter;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by James on 22/01/2018.
 */

public class UseTokenViewModel extends BaseViewModel {
    private static final long CYCLE_SIGNATURE_INTERVAL = 30;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final UseTokenInteract useTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MyTokensRouter myTokensRouter;
    private final TicketTransferRouter ticketTransferRouter;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final SignatureDisplayRouter signatureDisplayRouter;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<String> signature = new MutableLiveData<>();

    @Nullable
    private Disposable cycleSignatureDisposable;

    UseTokenViewModel(
            UseTokenInteract useTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            MyTokensRouter myTokensRouter,
            TicketTransferRouter ticketTransferRouter,
            SignatureDisplayRouter signatureDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.useTokenInteract = useTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.myTokensRouter = myTokensRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.signatureDisplayRouter = signatureDisplayRouter;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.ticketTransferRouter = ticketTransferRouter;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<String> signature() {
        return signature;
    }

    public void showRotatingSignature(Context context) {
        signatureDisplayRouter.open(context, defaultWallet.getValue());

    }

    public void prepare() {
        progress.postValue(true);
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

    private void onWalletFetchComplete() {

    }

    public void startCycleSignature() {
        progress.postValue(true);
        cycleSignatureDisposable = Observable.interval(0, CYCLE_SIGNATURE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> signatureGenerateInteract
                        .getMessage(defaultWallet.getValue()/*new Wallet("0x60f7a1cbc59470b74b1df20b133700ec381f15d3")*/)
                        .subscribe(this::onSignedMessage, this::onError))
                .subscribe(l -> {}, t -> {});
    }

    private void onMessageGenerated() {
        progress.postValue(false);
    }

    private void onSignedMessage(String message) {
        progress.postValue(false);
        //write to screen
        signature.setValue(message);
    }

    public void showTransferToken(Context context, String address) {
        ticketTransferRouter.open(context, address);
    }

    private void onDefaultWallet(Wallet wallet) {
        //TODO: switch on 'use' button
        progress.postValue(false);
        defaultWallet.setValue(wallet);
    }
}
