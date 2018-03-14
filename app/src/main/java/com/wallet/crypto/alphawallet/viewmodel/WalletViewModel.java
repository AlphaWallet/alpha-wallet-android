package com.wallet.crypto.alphawallet.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import com.wallet.crypto.alphawallet.router.AddTokenRouter;
import com.wallet.crypto.alphawallet.router.AssetDisplayRouter;
import com.wallet.crypto.alphawallet.router.ChangeTokenCollectionRouter;
import com.wallet.crypto.alphawallet.router.SendTokenRouter;
import com.wallet.crypto.alphawallet.router.TransactionsRouter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.Disposable;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

public class WalletViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

//    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final TransactionsRouter transactionsRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;

    private Token[] tokenCache = null;

    @Nullable
    private Disposable getBalanceDisposable;
    @Nullable
    private Disposable fetchTransactionDisposable;
    private Handler handler = new Handler();

    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            TransactionsRouter transactionsRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter,
            AssetDisplayRouter assetDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.transactionsRouter = transactionsRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
    }

//    public MutableLiveData<Wallet> wallet() {
//        return wallet;
//    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }

    public LiveData<BigDecimal> total() {
        return total;
    }

    public void fetchTokens() {
        progress.postValue(true);
        fetchTokensInteract
                .fetch(defaultWallet.getValue())
                .subscribe(this::onTokens, this::onError, this::onFetchTokensCompletable);
    }

    private void onFetchTokensCompletable() {
        this.tokens.setValue(tokenCache);
        progress.postValue(false);
        Token[] tokens = tokens().getValue();
        if (tokens == null || tokens.length == 0) {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
    }

    private void onTokens(Token[] tokens) {
        tokenCache = tokens;
        if (tokens != null && tokens.length > 0) {
            progress.postValue(true);
            showTotalBalance(tokenCache);
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
        if (address.equalsIgnoreCase(defaultWallet().getValue().address)) isToken = false;
        sendTokenRouter.open(context, address, symbol, decimals, isToken);
//        showTransactions(context);
    }

    @Override
    public void showRedeemToken(Context context, Token token) {
        assetDisplayRouter.open(context, token);
    }

    public void showTransactions(Context context) {
        transactionsRouter.open(context, true);
    }

    public void showEditTokens(Context context) {
        changeTokenCollectionRouter.open(context, defaultWallet.getValue());
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        handler.removeCallbacks(startGetBalanceTask);
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Map<String, String>> defaultWalletBalance() {
        return defaultWalletBalance;
    }

    public void prepare() {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public void getBalance() {
        getBalanceDisposable = getDefaultWalletBalance
                .get(defaultWallet.getValue())
                .subscribe(values -> {
                    defaultWalletBalance.postValue(values);
                    handler.removeCallbacks(startGetBalanceTask);
                    handler.postDelayed(startGetBalanceTask, GET_BALANCE_INTERVAL);
                }, t -> {
                });
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    private final Runnable startGetBalanceTask = this::getBalance;
}
