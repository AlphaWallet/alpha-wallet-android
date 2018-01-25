package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;


import java.util.concurrent.TimeUnit;

import dagger.Provides;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by James on 25/01/2018.
 */

public class SignatureDisplayModel extends BaseViewModel {
    private static final long CYCLE_SIGNATURE_INTERVAL = 30;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<String> signature = new MutableLiveData<>();

    @Nullable
    private Disposable cycleSignatureDisposable;

    SignatureDisplayModel(
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<String> signature() {
        return signature;
    }


    @Override
    protected void onCleared() {
        super.onCleared();

        if (cycleSignatureDisposable != null) {
            cycleSignatureDisposable.dispose();
        }
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
        cycleSignatureDisposable = Observable.interval(0, CYCLE_SIGNATURE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> signatureGenerateInteract
                        .getMessage(defaultWallet.getValue()/*new Wallet("0x60f7a1cbc59470b74b1df20b133700ec381f15d3")*/)
                        .subscribe(this::onSignedMessage, this::onError))
                .subscribe(l -> {}, t -> {});
    }

    private void onMessageGenerated() {

    }

    private void onSignedMessage(String message) {

        //write to screen
        signature.postValue(message);
    }

    private void onDefaultWallet(Wallet wallet) {
        //TODO: switch on 'use' button

        defaultWallet.setValue(wallet);
    }
}
