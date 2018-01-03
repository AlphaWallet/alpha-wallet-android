package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.trustapp.entity.GasSettings;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.CreateTransactionInteract;
import com.wallet.crypto.trustapp.interact.FetchGasSettingsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.router.GasSettingsRouter;

import java.math.BigInteger;

public class ConfirmationViewModel extends BaseViewModel {
    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final CreateTransactionInteract createTransactionInteract;

    private final GasSettingsRouter gasSettingsRouter;

    public ConfirmationViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                                 FetchGasSettingsInteract fetchGasSettingsInteract,
                                 CreateTransactionInteract createTransactionInteract,
                                 GasSettingsRouter gasSettingsRouter) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
    }

    public void createTransaction(String from, String to, String amount, BigInteger gasPrice, BigInteger gasLimit) {
        progress.postValue(true);
        disposable = createTransactionInteract
                .create(new Wallet(from), to, amount, gasPrice, gasLimit)
                .subscribe(this::onCreateTransaction, this::onError);
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<GasSettings> gasSettings() {
        return gasSettings;
    }

    public LiveData<String> sendTransaction() { return newTransaction; }

    public void prepare() {
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onCreateTransaction(String transaction) {
        progress.postValue(false);
        newTransaction.postValue(transaction);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        onGasSettings(fetchGasSettingsInteract.fetch());
    }

    private void onGasSettings(GasSettings gasSettings) {
        this.gasSettings.setValue(gasSettings);
    }

    public void openGasSettings(Context context) {
        gasSettingsRouter.open(context);
    }
}
