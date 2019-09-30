package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.SellDetailRouter;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by James on 16/02/2018.
 */

public class SellTicketModel  extends BaseViewModel {
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final SellDetailRouter sellDetailRouter;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Token> ticket = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;

    SellTicketModel(
            FetchTokensInteract fetchTokensInteract,
            GenericWalletInteract genericWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            SellDetailRouter sellDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.sellDetailRouter = sellDetailRouter;
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
        return ticket;
    }

    public void fetchCurrentTicketBalance() {
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetchSingle(defaultWallet.getValue(), ticket().getValue())
                        .subscribe(this::onToken, t -> {}))
                .subscribe(l -> {}, t -> {});
    }

    public void prepare(Token t) {
        ticket.setValue(t);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onToken(Token t)
    {
        ticket.setValue(t);
        ticket.postValue(t);
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

    public void openMarketDialog(Context context, String ticketIDs) {
        try {
            Token ticket = this.ticket().getValue();
            sellDetailRouter.openMarketPlace(context, ticket, ticketIDs, defaultWallet.getValue());
        } catch (Exception e) {

        }
    }

    public void openUniversalLinkDialog(Context context, String selection)
    {
        try
        {
            Token ticket = this.ticket().getValue();
            sellDetailRouter.openUniversalLink(context, ticket, selection, defaultWallet.getValue(), SellDetailActivity.SET_A_PRICE, 0);
        }
        catch (Exception e)
        {

        }
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
