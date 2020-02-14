package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.ExternalBrowserRouter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.service.TokensService;

import java.math.BigInteger;

public class TransactionDetailViewModel extends BaseViewModel {

    private final ExternalBrowserRouter externalBrowserRouter;
    private final TokensService tokenService;
    private final FindDefaultNetworkInteract networkInteract;
    private final TokenRepositoryType tokenRepository;

    private final MutableLiveData<BigInteger> lastestBlock = new MutableLiveData<>();
    public LiveData<BigInteger> latestBlock() {
        return lastestBlock;
    }

    TransactionDetailViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ExternalBrowserRouter externalBrowserRouter,
            TokenRepositoryType tokenRepository,
            TokensService service) {
        this.networkInteract = findDefaultNetworkInteract;
        this.externalBrowserRouter = externalBrowserRouter;
        this.tokenService = service;
        this.tokenRepository = tokenRepository;
    }

    public void prepare(int chainId)
    {
        disposable = tokenRepository.fetchLatestBlockNumber(chainId)
                .subscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(lastestBlock::postValue, t -> { this.lastestBlock.postValue(BigInteger.ZERO); });
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

    public Token getToken(int chainId, String address)
    {
        return tokenService.getToken(chainId, address);
    }

    @Nullable
    private Uri buildEtherscanUri(Transaction transaction) {
        NetworkInfo networkInfo = networkInteract.getNetworkInfo(transaction.chainId);
        if (networkInfo != null && !TextUtils.isEmpty(networkInfo.etherscanUrl)) {
            return Uri.parse(networkInfo.etherscanUrl)
                    .buildUpon()
                    .appendEncodedPath(transaction.hash)
                    .build();
        }
        return null;
    }

    public boolean hasEtherscanDetail(Transaction tx)
    {
        NetworkInfo networkInfo = networkInteract.getNetworkInfo(tx.chainId);
        return networkInfo.etherscanUrl != null && networkInfo.etherscanUrl.length() != 0;
    }

    public String getNetworkName(int chainId)
    {
        return networkInteract.getNetworkName(chainId);
    }

    public String getNetworkSymbol(int chainId)
    {
        return networkInteract.getNetworkInfo(chainId).symbol;
    }
}
