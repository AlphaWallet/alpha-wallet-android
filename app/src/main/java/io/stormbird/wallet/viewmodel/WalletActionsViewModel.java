package io.stormbird.wallet.viewmodel;

import android.util.Log;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchWalletsInteract;

public class WalletActionsViewModel extends BaseViewModel {
    private final static String TAG = WalletActionsViewModel.class.getSimpleName();

    private final FetchWalletsInteract fetchWalletsInteract;

    WalletActionsViewModel(
            FetchWalletsInteract fetchWalletsInteract) {
        this.fetchWalletsInteract = fetchWalletsInteract;
    }

    public void storeWallet(Wallet wallet) {
        disposable = fetchWalletsInteract.storeWallet(wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onStored, this::onError);
    }

    private void onStored(Integer count) {
        Log.d(TAG, "Stored " + count + " Wallets");
    }
}
