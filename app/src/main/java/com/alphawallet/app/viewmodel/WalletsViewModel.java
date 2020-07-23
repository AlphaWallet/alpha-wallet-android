package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetDefaultWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.AWEnsResolver;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

public class WalletsViewModel extends BaseViewModel
{
    private final static String TAG = WalletsViewModel.class.getSimpleName();

    private static final int BALANCE_CHECK_INTERVAL_SECONDS = 20;

    private final SetDefaultWalletInteract setDefaultWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final KeyService keyService;
    private final GasService gasService;
    private final Context context;

    private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;
    private final TokensService tokensService;
    private final AWEnsResolver ensResolver;

    private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noWalletsError = new MutableLiveData<>();
    private final MutableLiveData<Wallet> updateBalance = new MutableLiveData<>();
    private final MutableLiveData<Wallet> updateENSName = new MutableLiveData<>();

    private NetworkInfo currentNetwork;
    private Map<String, Wallet> walletBalances = new HashMap<>();
    private int walletUpdateCount;

    @Nullable
    private Disposable balanceTimerDisposable;

    WalletsViewModel(
            SetDefaultWalletInteract setDefaultWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            GenericWalletInteract genericWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            KeyService keyService,
            GasService gasService,
            TokensService tokensService,
            Context context)
    {
        this.setDefaultWalletInteract = setDefaultWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.importWalletRouter = importWalletRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.keyService = keyService;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.context = context;

        ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), context);
    }

    public LiveData<Wallet[]> wallets()
    {
        return wallets;
    }

    public LiveData<Wallet> defaultWallet()
    {
        return defaultWallet;
    }

    public LiveData<Wallet> createdWallet()
    {
        return createdWallet;
    }

    public LiveData<ErrorEnvelope> createWalletError()
    {
        return createWalletError;
    }
    public LiveData<Boolean> noWalletsError() { return noWalletsError; }

    public LiveData<Wallet> updateBalance()
    {
        return updateBalance;
    }

    public LiveData<Wallet> updateENSName() { return updateENSName; }

    public void setDefaultWallet(Wallet wallet)
    {
        disposable = setDefaultWalletInteract
                .set(wallet)
                .subscribe(() -> onDefaultWallet(wallet), this::onError);
    }

    private Wallet addWalletToMap(Wallet wallet) {
        if (wallet.address != null && !walletBalances.containsKey(wallet.address.toLowerCase())) walletBalances.put(wallet.address.toLowerCase(), wallet);
        return wallet;
    }

    public void onPrepare()
    {
        walletBalances.clear();
        progress.postValue(true);
        ContractLocator override = EthereumNetworkRepository.getOverrideToken();
        currentNetwork = findDefaultNetworkInteract.getNetworkInfo(override.chainId);

        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet,
                           error -> noWalletsError.postValue(true));
    }

    private void onDefaultWallet(Wallet wallet)
    {
        defaultWallet.postValue(wallet);
        disposable = fetchWalletsInteract
                .fetch()
                .subscribe(this::onWallets, this::onError);
    }

    private void updateWallets()
    {
        //now load the current wallets from database
        disposable = fetchWalletsInteract
                .fetch()
                .subscribe(this::getWalletsBalance, this::onError);
    }

    private void onWallets(Wallet[] items)
    {
        progress.postValue(false);

        for (Wallet w : items)
        {
            w.balanceSymbol = currentNetwork.symbol;
            Wallet mapW = walletBalances.get(w.address.toLowerCase());
            if (mapW != null)
            {
                w.balance = mapW.balance;
            }
        }
        wallets.postValue(items);

        startBalanceUpdateTimer(items);
    }

    public void updateBalancesIfRequired(Wallet[] wallets)
    {
        if (walletBalances.size() == 0)
        {
            getWalletsBalance(wallets);
        }
    }

    public void swipeRefreshWallets()
    {
        //check for updates
        //check names first
        disposable = fetchWalletsInteract.fetch().toObservable()
                .flatMap(Observable::fromArray)
                .forEach(wallet -> ensResolver.resolveEnsName(wallet.address)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(ensName -> updateOnName(wallet, ensName), this::onError));

        updateWallets();
    }

    private void updateOnName(Wallet wallet, String ensName)
    {
        if (!TextUtils.isEmpty(ensName))
        {
            wallet.ENSname = ensName;
            disposable = fetchWalletsInteract.updateWalletData(wallet)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(updateENSName::postValue, this::onError);
        }
    }

    public void fetchWallets()
    {
        progress.postValue(true);
        onPrepare();
    }

    public void newWallet(Activity ctx, CreateWalletCallbackInterface createCallback)
    {
        Completable.fromAction(() -> keyService.createNewHDKey(ctx, createCallback)) //create wallet on a computation thread to give UI a chance to complete all tasks
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .isDisposed();
    }

    private void startBalanceUpdateTimer(final Wallet[] wallets)
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed()) balanceTimerDisposable.dispose();

        balanceTimerDisposable = Observable.interval(0, BALANCE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS)
                .doOnNext(l -> getWalletsBalance(wallets)).subscribe();
    }

    /**
     * Sequentially updates the wallet balances on the current network
     *
     * @param wallets
     */
    private void getWalletsBalance(Wallet[] wallets)
    {
        ContractLocator override = EthereumNetworkRepository.getOverrideToken();
        NetworkInfo    network  = findDefaultNetworkInteract.getNetworkInfo(override.chainId);
        walletUpdateCount = wallets.length;

        disposable = Observable.fromArray(wallets)
                .map(this::addWalletToMap)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(wallet -> {
                    fetchTokensInteract.fetchStoredToken(network, wallet, override.address) //fetch cached balance from this wallet's DB
                    .flatMap(tokenFromCache -> fetchTokensInteract.updateBalance(wallet.address, tokenFromCache)) //update balance
                    .subscribe(this::updateWallet, error -> onFetchError(wallet)).isDisposed();
                }, this::onError, () -> progress.postValue(false));
    }

    private void onFetchError(Wallet wallet)
    {
        //leave wallet with stale value, but grey out balance
        if (walletBalances.containsKey(wallet.address.toLowerCase()))
        {
            wallet.balance = "*" + wallet.balance; //stale balance annotation - only used in WalletHolder
            updateBalance.postValue(wallet);
            wallet.balance = wallet.balance.substring(1); //don't store the stale balance annotation
            walletBalances.put(wallet.address.toLowerCase(), wallet);
        }
        storeWallets();
    }

    private void updateWallet(Token token)
    {
        Wallet wallet = walletBalances.get(token.getWallet().toLowerCase());
        if (wallet != null)
        {
            wallet.setWalletBalance(token);
            updateBalance.postValue(wallet);
        }
        storeWallets();
    }

    private void storeWallets()
    {
        walletUpdateCount--;
        if (walletUpdateCount == 0)
        {
            //write wallets to DB
            disposable = fetchWalletsInteract.storeWallets(walletBalances.values().toArray(new Wallet[0]))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onStored, this::onError);
        }
    }

    private void onStored(Wallet[] wallets)
    {
        Log.d(TAG, "Stored " + wallets.length + " Wallets");
    }

    private void onCreateWalletError(Throwable throwable)
    {
        progress.postValue(false);
        createWalletError.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
    }

    public void importWallet(Activity activity)
    {
        importWalletRouter.openForResult(activity, C.IMPORT_REQUEST_CODE);
    }

    public void showHome(Context context)
    {
        homeRouter.open(context, true);
    }

    public NetworkInfo getNetwork()
    {
        return currentNetwork;
    }

    public void StoreHDWallet(String address, KeyService.AuthenticationLevel authLevel)
    {
        if (!address.equals(ZERO_ADDRESS))
        {
            Wallet wallet = new Wallet(address);
            wallet.type = WalletType.HDKEY;
            wallet.authLevel = authLevel;
            fetchWalletsInteract.storeWallet(wallet)
                    .subscribe(account -> {
                        fetchWallets();
                        createdWallet.postValue(account);
                    }, this::onCreateWalletError).isDisposed();
        }
    }

    public void watchWallet(Activity activity)
    {
        importWalletRouter.openWatchCreate(activity, C.IMPORT_REQUEST_CODE);
    }

    public void completeAuthentication(Operation taskCode)
    {
        keyService.completeAuthentication(taskCode);
    }

    public void failedAuthentication(Operation taskCode)
    {
        keyService.failedAuthentication(taskCode);
    }

    public void onPause()
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed()) balanceTimerDisposable.dispose();
        balanceTimerDisposable = null;
    }
}
