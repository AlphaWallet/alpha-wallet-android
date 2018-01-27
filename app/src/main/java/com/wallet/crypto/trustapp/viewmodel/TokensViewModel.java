package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.FetchTokensInteract;
import com.wallet.crypto.trustapp.router.AddTokenRouter;
import com.wallet.crypto.trustapp.router.ChangeTokenCollectionRouter;
import com.wallet.crypto.trustapp.router.SendTokenRouter;
import com.wallet.crypto.trustapp.router.TransactionsRouter;

import java.math.BigDecimal;

import static com.wallet.crypto.trustapp.C.ErrorCode.EMPTY_COLLECTION;

public class TokensViewModel extends BaseViewModel {
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final TransactionsRouter transactionsRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;

    TokensViewModel(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            TransactionsRouter transactionsRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.transactionsRouter = transactionsRouter;
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

            if (tokens.length > 1 && tokens[1].ticker == null) {
                return; // Show than have ticker for tokens.
            }
            BigDecimal total = new BigDecimal("0");
            for (Token token : tokens) {
                if (token.balance != null
                        && token.ticker != null
                        && token.balance.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, token.tokenInfo.decimals));
                    BigDecimal ethBalance = token.tokenInfo.decimals > 0
                            ? token.balance.divide(decimalDivisor, BigDecimal.ROUND_HALF_UP)
                            : token.balance;
                    total = total.add(ethBalance.multiply(new BigDecimal(token.ticker.price)));
                }
            }
            total = total.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
            if (total.compareTo(BigDecimal.ZERO) != 0) {
                this.total.postValue(total);
            }
        }
    }

    public void showAddToken(Context context) {
        addTokenRouter.open(context);
    }

    public void showSendToken(Context context, String address, String symbol, int decimals) {
        sendTokenRouter.open(context, address, symbol, decimals);

    }

    public void showTransactions(Context context) {
        transactionsRouter.open(context, true);
    }

    public void showEditTokens(Context context) {
        changeTokenCollectionRouter.open(context, wallet.getValue());
    }
}
