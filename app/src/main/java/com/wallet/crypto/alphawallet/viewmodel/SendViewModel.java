package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;

import java.math.BigInteger;

public class SendViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();

    private final ConfirmationRouter confirmationRouter;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;

    public SendViewModel(ConfirmationRouter confirmationRouter, FetchGasSettingsInteract fetchGasSettingsInteract) {
        this.confirmationRouter = confirmationRouter;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
    }

    public void openConfirmation(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens) {
        confirmationRouter.open(context, to, amount, contractAddress, decimals, symbol, sendingTokens);
    }
}
