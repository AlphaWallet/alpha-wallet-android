package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.ExternalBrowserRouter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.stormbird.wallet.service.TokensService;

public class TransactionDetailViewModel extends BaseViewModel {

    private final ExternalBrowserRouter externalBrowserRouter;
    private final TokensService tokenService;
    private final FindDefaultNetworkInteract networkInteract;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    TransactionDetailViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ExternalBrowserRouter externalBrowserRouter,
            TokensService service) {
        this.networkInteract = findDefaultNetworkInteract;
        this.externalBrowserRouter = externalBrowserRouter;
        this.tokenService = service;

        disposable = findDefaultWalletInteract
                .find()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(defaultWallet::postValue,  t -> {});
    }

    public void showMoreDetails(Context context, Transaction transaction) {
        Uri uri = buildEtherscanUri(transaction, false);
        if (uri != null) {
            externalBrowserRouter.open(context, uri);
        }
    }

    public void shareTransactionDetail(Context context, Transaction transaction) {
        Uri shareUri = buildEtherscanUri(transaction, false);
        if (shareUri != null) {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.subject_transaction_detail));
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareUri.toString());
            context.startActivity(Intent.createChooser(sharingIntent, "Share via"));
        }
    }

    public Token getToken(int chainId, String address)
    {
        return tokenService.getToken(chainId, address);
    }

    @Nullable
    private Uri buildEtherscanUri(Transaction transaction, boolean erc20EventAPI) {
        NetworkInfo networkInfo = networkInteract.getNetworkInfo(transaction.chainId, erc20EventAPI);
        if (networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanUrl)) {
            return Uri.parse(networkInfo.etherscanUrl)
                    .buildUpon()
                    .appendEncodedPath(transaction.hash)
                    .build();
        }
        return null;
    }

    public String getNetworkName(int chainId)
    {
        return networkInteract.getNetworkName(chainId);
    }

    public String getNetworkSymbol(int chainId)
    {
        return networkInteract.getNetworkInfo(chainId, false).symbol;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
}
