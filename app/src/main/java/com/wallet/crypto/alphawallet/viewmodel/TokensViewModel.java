package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.router.AddTokenRouter;
import com.wallet.crypto.alphawallet.router.ChangeTokenCollectionRouter;
import com.wallet.crypto.alphawallet.router.HomeRouter;
import com.wallet.crypto.alphawallet.router.SendTokenRouter;
import com.wallet.crypto.alphawallet.router.TransactionsRouter;
import com.wallet.crypto.alphawallet.router.AssetDisplayRouter;

import java.math.BigDecimal;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

public class TokensViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final HomeRouter homeRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;

    TokensViewModel(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            HomeRouter homeRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter,
            AssetDisplayRouter assetDisplayRouter) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.homeRouter = homeRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
    }

    public MutableLiveData<Wallet> wallet() {
        return wallet;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }

    public LiveData<BigDecimal> total() {
        return total;
    }

    public void fetchTokens() {
        progress.postValue(true);
        fetchTokensInteract
                .fetch(wallet.getValue())
                .subscribe(this::onTokens, this::onError, this::onFetchTokensCompletable);
    }

    private void onFetchTokensCompletable() {
        progress.postValue(false);
        Token[] tokens = tokens().getValue();
        if (tokens == null || tokens.length == 0) {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
    }

    private void onTokens(Token[] tokens) {
        this.tokens.setValue(tokens);
        if (tokens != null && tokens.length > 0) {
            progress.postValue(true);
            showTotalBalance(tokens);
        }
    }

    private void showTotalBalance(Token[] tokens) {
        BigDecimal total = new BigDecimal("0");
        for (Token token : tokens) {
            if (token.balance != null && token.ticker != null
                    && token.balance.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, token.tokenInfo.decimals));
                BigDecimal ethBalance = token.tokenInfo.decimals > 0
                        ? token.balance.divide(decimalDivisor)
                        : token.balance;
                total = total.add(ethBalance.multiply(new BigDecimal(token.ticker.price)));
            }
        }
        total = total.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            total = null;
        }
        this.total.postValue(total);
    }

    public void showAddToken(Context context) {
        addTokenRouter.open(context);
    }

    @Override
    public void showSendToken(Context context, String address, String symbol, int decimals) {
        boolean isToken = true;
        if (address.equalsIgnoreCase(wallet().getValue().address)) isToken = false;
        sendTokenRouter.open(context, address, symbol, decimals, isToken);
    }
    @Override
    public void showRedeemToken(Context context, Token token) {
        assetDisplayRouter.open(context, token);
    }

    public void showTransactions(Context context) {
        homeRouter.open(context, true);
    }

    public void showEditTokens(Context context) {
        changeTokenCollectionRouter.open(context, wallet.getValue());
    }
}
