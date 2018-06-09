package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.ui.SendActivity;

import java.math.BigInteger;

public class SendViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction> transaction = new MutableLiveData<>();

    private final ConfirmationRouter confirmationRouter;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final MyAddressRouter myAddressRouter;

    public SendViewModel(ConfirmationRouter confirmationRouter,
                         FetchGasSettingsInteract fetchGasSettingsInteract,
                         MyAddressRouter myAddressRouter) {
        this.confirmationRouter = confirmationRouter;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.myAddressRouter = myAddressRouter;
    }

    public void openConfirmation(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens) {
        confirmationRouter.open(context, to, amount, contractAddress, decimals, symbol, sendingTokens);
    }

    public void showMyAddress(Context context, Wallet wallet) {
        myAddressRouter.open(context, wallet);
    }

    public void showContractInfo(Context ctx, String contractAddress)
    {
        myAddressRouter.open(ctx, contractAddress);
    }
}
