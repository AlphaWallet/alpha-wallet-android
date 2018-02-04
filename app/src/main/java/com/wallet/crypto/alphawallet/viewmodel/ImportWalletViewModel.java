package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.ServiceErrorException;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.ImportWalletInteract;
import com.wallet.crypto.alphawallet.ui.widget.OnImportKeystoreListener;
import com.wallet.crypto.alphawallet.ui.widget.OnImportPrivateKeyListener;

public class ImportWalletViewModel extends BaseViewModel implements OnImportKeystoreListener, OnImportPrivateKeyListener {

    private final ImportWalletInteract importWalletInteract;
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();

    ImportWalletViewModel(ImportWalletInteract importWalletInteract) {
        this.importWalletInteract = importWalletInteract;
    }

    @Override
    public void onKeystore(String keystore, String password) {
        progress.postValue(true);
        importWalletInteract
                .importKeystore(keystore, password)
                .subscribe(this::onWallet, this::onError);
    }

    @Override
    public void onPrivateKey(String key) {
        progress.postValue(true);
        importWalletInteract
                .importPrivateKey(key)
                .subscribe(this::onWallet, this::onError);
    }

    public LiveData<Wallet> wallet() {
        return wallet;
    }

    private void onWallet(Wallet wallet) {
        progress.postValue(false);
        this.wallet.postValue(wallet);
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
}
