package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.alphawallet.app.ui.TransferTicketDetailActivity;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.TransferTicketDetailRouter;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import com.alphawallet.app.service.AssetDefinitionService;

public class TransferTicketViewModel extends BaseViewModel {
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Token> token = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;

    TransferTicketViewModel(
            FetchTokensInteract fetchTokensInteract,
            GenericWalletInteract genericWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            TransferTicketDetailRouter transferTicketDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public LiveData<Token> ticket() {
        return token;
    }

    public void fetchCurrentTicketBalance() {
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetchSingle(defaultWallet.getValue(), ticket().getValue())
                        .subscribe(this::onToken, t -> {}))
                .subscribe(l -> {}, t -> {});
    }

    public void prepare(Token t) {
        token.setValue(t);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onToken(Token t)
    {
        token.postValue(t);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        //TODO: switch on 'use' button
        progress.postValue(false);
        defaultWallet.setValue(wallet);
        fetchCurrentTicketBalance();
    }

    public void openSellDialog(Context context, String ticketIDs)
    {
        transferTicketDetailRouter.open(context, token.getValue(), ticketIDs, defaultWallet.getValue());
    }

    public void openTransferDirectDialog(Context context, String tokenId)
    {
        transferTicketDetailRouter.openTransfer(context, token.getValue(), tokenId, defaultWallet.getValue(), TransferTicketDetailActivity.TRANSFER_TO_ADDRESS);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
