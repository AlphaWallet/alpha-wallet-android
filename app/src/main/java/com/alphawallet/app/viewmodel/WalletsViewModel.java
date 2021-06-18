package com.alphawallet.app.viewmodel;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Context;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetDefaultWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.AWEnsResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class WalletsViewModel extends BaseViewModel
{
    private final static String TAG = WalletsViewModel.class.getSimpleName();

    private static final int BALANCE_CHECK_INTERVAL_SECONDS = 20;

    private final SetDefaultWalletInteract setDefaultWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final KeyService keyService;
    private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;
    private final TokensService tokensService;
    private final AWEnsResolver ensResolver;
    private final AssetDefinitionService assetService;

    private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noWalletsError = new MutableLiveData<>();

    private NetworkInfo currentNetwork;
    private Map<String, Wallet> walletBalances = new HashMap<>();

    @Nullable
    private Disposable balanceTimerDisposable;

    WalletsViewModel(
            SetDefaultWalletInteract setDefaultWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            GenericWalletInteract genericWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            KeyService keyService,
            TokensService tokensService,
            AssetDefinitionService assetService,
            Context context)
    {
        this.setDefaultWalletInteract = setDefaultWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.importWalletRouter = importWalletRouter;
        this.homeRouter = homeRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.keyService = keyService;
        this.tokensService = tokensService;
        this.assetService = assetService;

        ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), context);
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

    public void setDefaultWallet(Wallet wallet)
    {
        disposable = setDefaultWalletInteract
                .set(wallet)
                .subscribe(() -> onDefaultWallet(wallet), this::onError);
    }

    public void onPrepare(int chainId)
    {
        walletBalances.clear();
        progress.postValue(true);
        currentNetwork = findDefaultNetworkInteract.getNetworkInfo(chainId);

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

    public void swipeRefreshWallets()
    {
        //check for updates
        //check names first
        disposable = fetchWalletsInteract.fetch().toObservable()
                .flatMap(Observable::fromArray)
                .forEach(wallet -> ensResolver.resolveEnsName(wallet.address)
                        .map(ensName -> { wallet.ENSname = ensName; return wallet;})
                        .flatMap(fetchWalletsInteract::updateWalletData)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(w -> { }, this::onError).isDisposed());

        updateWallets();
    }

    public void fetchWallets()
    {
        progress.postValue(true);
        onPrepare(currentNetwork.chainId);
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

        balanceTimerDisposable = Observable.interval(1, BALANCE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS) //initial delay 1 second to allow view to stabilise
                .doOnNext(l -> getWalletsBalance(wallets)).subscribe();
    }

    /**
     * Sequentially updates the wallet balances on the current network and stores to database if necessary
     *
     * @param wallets - array of wallets
     */
    private void getWalletsBalance(Wallet[] wallets)
    {
        //loop through wallets and update balance
        disposable = Observable.fromArray(wallets)
                .forEach(wallet -> tokensService.getChainBalance(wallet.address.toLowerCase(), currentNetwork.chainId)
                        .flatMap(newBalance -> genericWalletInteract.updateBalanceIfRequired(wallet, newBalance))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(w -> { }, e -> { })
                        .isDisposed());

        progress.postValue(false);
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
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
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

    public GenericWalletInteract getWalletInteract()
    {
        return genericWalletInteract;
    }

    public void stopUpdates()
    {
        assetService.stopEventListener();
    }
}
