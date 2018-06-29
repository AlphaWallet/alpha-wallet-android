package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SignatureGenerateInteract;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.RedeemAssetSelectRouter;
import io.stormbird.wallet.router.MyTokensRouter;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketRouter;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.AssetDisplayActivity;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayViewModel extends BaseViewModel
{
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private static final String TAG = "ADVM";
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MyTokensRouter myTokensRouter;
    private final TransferTicketRouter transferTicketRouter;
    private final RedeemAssetSelectRouter redeemAssetSelectRouter;
    private final SellTicketRouter sellTicketRouter;
    private final MyAddressRouter myAddressRouter;
    private final AssetDefinitionService assetDefinitionService;
    
    private final HomeRouter homeRouter;
    private Token refreshToken;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Token> ticket = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;

    AssetDisplayViewModel(
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            MyTokensRouter myTokensRouter,
            TransferTicketRouter transferTicketRouter,
            RedeemAssetSelectRouter redeemAssetSelectRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            SellTicketRouter sellTicketRouter,
            HomeRouter homeRouter,
            MyAddressRouter myAddressRouter,
            AssetDefinitionService assetDefinitionService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.myTokensRouter = myTokensRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.redeemAssetSelectRouter = redeemAssetSelectRouter;
        this.transferTicketRouter = transferTicketRouter;
        this.sellTicketRouter = sellTicketRouter;
        this.homeRouter = homeRouter;
        this.myAddressRouter = myAddressRouter;
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

    public void selectAssetIdsToRedeem(Context context, Ticket token) {
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        redeemAssetSelectRouter.open(context, token);
    }

    public void fetchCurrentTicketBalance() {
        if (getBalanceDisposable != null) getBalanceDisposable.dispose();
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> fetchTokensInteract
                        .fetchSingle(defaultWallet.getValue(), ticket().getValue())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onToken, this::onError, this::finishTokenFetch)).subscribe();
    }

    private void finishTokenFetch()
    {
        Log.d(TAG, "refreshToken: " + refreshToken.tokenInfo.name );
        ticket.postValue(refreshToken);
    }

    public void prepare(Token t) {
        ticket.setValue(t);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    private void onToken(Token t)
    {
        refreshToken = t;
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void showTransferToken(Context context, Ticket ticket) {
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
        transferTicketRouter.open(context, ticket);
    }

    public void showContractInfo(Context ctx, String address)
    {
        myAddressRouter.open(ctx, address);
    }

    public void sellTicketRouter(Context ctx, Ticket ticket) {
        sellTicketRouter.open(ctx, ticket);
    }

    public void showTransferToken(Context context, Ticket ticket, TicketRange range) {
//        transferTicketRouter.openRange(context, ticket, range);
    }

    private void onDefaultWallet(Wallet wallet) {
        //TODO: switch on 'use' button
        progress.postValue(false);
        defaultWallet.setValue(wallet);
        fetchCurrentTicketBalance();
    }

    public void showHome(Context context, boolean isClearStack) {
        homeRouter.open(context, isClearStack);
    }
}
