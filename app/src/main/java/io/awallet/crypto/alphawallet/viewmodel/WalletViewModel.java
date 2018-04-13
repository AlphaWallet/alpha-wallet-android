package io.awallet.crypto.alphawallet.viewmodel;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import org.web3j.abi.datatypes.Bool;

import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.router.AddTokenRouter;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;
import io.awallet.crypto.alphawallet.router.ChangeTokenCollectionRouter;
import io.awallet.crypto.alphawallet.router.SendTokenRouter;

import java.math.BigDecimal;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static io.awallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

public class WalletViewModel extends BaseViewModel {
    private static final long GET_BALANCE_INTERVAL = 10 * DateUtils.SECOND_IN_MILLIS;

//    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<BigDecimal> total = new MutableLiveData<>();
    private final MutableLiveData<Token> tokenUpdate = new MutableLiveData<>();
    private final MutableLiveData<Boolean> checkTokens = new MutableLiveData<>();

    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> defaultWalletBalance = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;

    private Token[] tokenCache = null;

    private Handler handler = new Handler();

    @Nullable
    private Disposable fetchTokenBalanceDisposable;

    WalletViewModel(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter,
            AssetDisplayRouter assetDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }
    public LiveData<BigDecimal> total() {
        return total;
    }
    public LiveData<Token> tokenUpdate() { return tokenUpdate; }
    public LiveData<Boolean> endUpdate() { return checkTokens; }


    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(startUpdateBalanceTask);
        if (fetchTokenBalanceDisposable != null && !fetchTokenBalanceDisposable.isDisposed())
        {
            fetchTokenBalanceDisposable.dispose();
        }
    }

    //double cycle.
    //1. fetch tokens from cache and display
    //2. update token balance
    public void fetchTokens()
    {
        handler.removeCallbacks(startUpdateBalanceTask);
        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
        }

        if (defaultWallet.getValue() != null)
        {
            disposable = fetchTokensInteract
                    .fetchStoredWithEth(defaultWallet.getValue())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokens, this::onError, this::onFetchTokensCompletable);
        }
        else
        {
            prepareWallet();
        }
    }

    private void onTokens(Token[] tokens)
    {
        tokenCache = tokens;
    }

    private void onFetchTokensCompletable()
    {
        progress.postValue(false);
        tokens.postValue(tokenCache);
        updateTokenBalances();
    }

    private void updateTokenBalances()
    {
        if (fetchTokenBalanceDisposable == null)
        {
            fetchTokenBalanceDisposable = fetchTokensInteract
                    .fetchSequential(defaultWallet.getValue())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onTokenBalanceUpdate, this::onError, this::onFetchTokensBalanceCompletable);
        }
    }

    private void onTokenBalanceUpdate(Token token)
    {
        if (token.hasPositiveBalance())
        {
            tokenUpdate.postValue(token);
        }
        //TODO: Calculate total value including token value received from token tickers
        //TODO: Then display the total value of everything at the top of the list in a special holder
    }

    private void onFetchTokensBalanceCompletable()
    {
        fetchTokenBalanceDisposable = null;
        progress.postValue(false);
        if (tokenCache != null && tokenCache.length > 0) {
            handler.removeCallbacks(startUpdateBalanceTask);
            handler.postDelayed(startUpdateBalanceTask, GET_BALANCE_INTERVAL);
            checkTokens.postValue(true);
        }
        else
        {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
    }

    //NB: This function is used to calculate total value of all tokens plus eth.
    //TODO: On mainnet, get tickers for all token values and calculate the overall $ value of all tokens + eth
//    private void showTotalBalance(Token[] tokens) {
//        BigDecimal total = new BigDecimal("0");
//        for (Token token : tokens) {
//            if (token.balance != null && token.ticker != null
//                    && token.balance.compareTo(BigDecimal.ZERO) != 0) {
//                BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, token.tokenInfo.decimals));
//                BigDecimal ethBalance = token.tokenInfo.decimals > 0
//                        ? token.balance.divide(decimalDivisor)
//                        : token.balance;
//                total = total.add(ethBalance.multiply(new BigDecimal(token.ticker.price)));
//            }
//        }
//        total = total.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
//        if (total.compareTo(BigDecimal.ZERO) == 0) {
//            total = null;
//        }
//        this.total.postValue(total);
//    }

    public void showAddToken(Context context) {
        addTokenRouter.open(context);
    }

    @Override
    public void showSendToken(Context context, String address, String symbol, int decimals, Token token) {
        boolean isToken = true;
        if (address.equalsIgnoreCase(defaultWallet().getValue().address)) isToken = false;
        sendTokenRouter.open(context, address, symbol, decimals, isToken, defaultWallet.getValue(), token);
    }

    @Override
    public void showRedeemToken(Context context, Token token) {
        assetDisplayRouter.open(context, token);
    }

    public void showEditTokens(Context context) {
        changeTokenCollectionRouter.open(context, defaultWallet.getValue());
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

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    private final Runnable startUpdateBalanceTask = this::updateTokenBalances;

    private void prepareWallet()
    {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetworkPrepare, this::onError);
    }

    private void onDefaultNetworkPrepare(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .toObservable()
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::onDefaultWallet, this::onError, this::fetchTokens);
    }
}
