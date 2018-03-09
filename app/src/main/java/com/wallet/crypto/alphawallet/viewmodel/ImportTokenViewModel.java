package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Base64;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.ServiceErrorException;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.ImportWalletInteract;
import com.wallet.crypto.alphawallet.service.ImportTokenService;
import com.wallet.crypto.alphawallet.ui.widget.OnImportKeystoreListener;
import com.wallet.crypto.alphawallet.ui.widget.OnImportPrivateKeyListener;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModel extends BaseViewModel  {

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final ImportTokenService importTokenService;

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private byte[] importData;

    ImportTokenViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                         ImportTokenService importTokenService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.importTokenService = importTokenService;
    }

    public void prepare(String importDataStr) {
        importData = Base64.decode(importDataStr.getBytes(), Base64.DEFAULT); //get import data
        progress.postValue(true);
        disposable = findDefaultWalletInteract
                .find()
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

    public void performImport()
    {
        try {
            importTokenService.importTickets(wallet.getValue(), importData);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //Wait for feedback. Refactor as an observable
    }
}
