package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;
import androidx.annotation.Nullable;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.TransferTicketDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;

import javax.inject.Inject;

@HiltViewModel
public class TransferTicketViewModel extends BaseViewModel {
    private static final long CHECK_BALANCE_INTERVAL = 10;
    private final TokensService tokensService;
    private final GenericWalletInteract genericWalletInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Token> token = new MutableLiveData<>();

    @Nullable
    private Disposable getBalanceDisposable;

    @Inject
    TransferTicketViewModel(
            TokensService tokensService,
            GenericWalletInteract genericWalletInteract,
            TransferTicketDetailRouter transferTicketDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        this.tokensService = tokensService;
        this.genericWalletInteract = genericWalletInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    public void prepare(Token t) {
        token.setValue(t);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        progress.postValue(false);
        defaultWallet.setValue(wallet);
        fetchCurrentTicketBalance();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (getBalanceDisposable != null) {
            getBalanceDisposable.dispose();
        }
    }

    public LiveData<Token> token() {
        return token;
    }

    public void fetchCurrentTicketBalance() {
        getBalanceDisposable = Observable.interval(CHECK_BALANCE_INTERVAL, CHECK_BALANCE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l -> {
                    Token t = tokensService.getToken(token.getValue().tokenInfo.chainId, token.getValue().getAddress());
                    onToken(t);
                }).subscribe(l -> {}, t -> {});
    }

    private void onToken(Token t)
    {
        token.postValue(t);
    }

    public void openSellDialog(Context context, String ticketIDs)
    {
        transferTicketDetailRouter.open(context, token.getValue(), ticketIDs, defaultWallet.getValue());
    }

    public void openTransferDirectDialog(Context context, String tokenId)
    {
        transferTicketDetailRouter.openTransfer(context, token.getValue(), tokenId, defaultWallet.getValue(), TRANSFER_TO_ADDRESS.ordinal());
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
