package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.router.ExternalBrowserRouter;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class TransactionDetailViewModel extends BaseViewModel {

    private final ExternalBrowserRouter externalBrowserRouter;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    TransactionDetailViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ExternalBrowserRouter externalBrowserRouter) {
        this.externalBrowserRouter = externalBrowserRouter;

        findDefaultNetworkInteract
                .find()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(defaultNetwork::postValue, t -> {});
        disposable = findDefaultWalletInteract
                .find()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(defaultWallet::postValue,  t -> {});
    }


    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public void showMoreDetails(Context context, Transaction transaction) {
        Uri uri = buildEtherscanUri(transaction);
        if (uri != null) {
            externalBrowserRouter.open(context, uri);
        }
    }

    public void shareTransactionDetail(Context context, Transaction transaction) {
        Uri shareUri = buildEtherscanUri(transaction);
        if (shareUri != null) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.subject_transaction_detail));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareUri.toString());
            context.startActivity(Intent.createChooser(sharingIntent, "Share via"));
        }
    }

    @Nullable
    private Uri buildEtherscanUri(Transaction transaction) {
        NetworkInfo networkInfo = defaultNetwork.getValue();
        if (networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanUrl)) {
            return Uri.parse(networkInfo.etherscanUrl)
                    .buildUpon()
                    .appendEncodedPath(transaction.hash)
                    .build();
        }
        return null;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
}
