package com.alphawallet.app.viewmodel;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.ServiceSyncCallback;
import com.alphawallet.app.entity.SyncCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetDefaultWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.AWEnsResolver;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@HiltViewModel
public class WalletsViewModel extends BaseViewModel implements ServiceSyncCallback
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

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;
    private final TickerService tickerService;

    private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noWalletsError = new MutableLiveData<>();

    private NetworkInfo currentNetwork;
    private final Map<String, Wallet> walletBalances = new HashMap<>();
    private final Map<String, TokensService> walletServices = new ConcurrentHashMap<>();
    private SyncCallback syncCallback;

    @Nullable
    private Disposable balanceTimerDisposable;

    @Nullable
    private Disposable walletBalanceUpdate;

    @Nullable
    private Disposable ensCheck;

    @Nullable
    private Disposable ensWrappingCheck;

    @Nullable
    private Disposable walletSync;

    @Inject
    WalletsViewModel(
            SetDefaultWalletInteract setDefaultWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            GenericWalletInteract genericWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            KeyService keyService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenRepositoryType tokenRepository,
            TickerService tickerService,
            AssetDefinitionService assetService,
            @ApplicationContext Context context)
    {
        this.setDefaultWalletInteract = setDefaultWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.importWalletRouter = importWalletRouter;
        this.homeRouter = homeRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.keyService = keyService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
        this.tickerService = tickerService;
        this.assetService = assetService;

        this.tokensService = new TokensService(ethereumNetworkRepository, tokenRepository, tickerService, null, null);

        ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), context);
        syncCallback = null;
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

    public void onPrepare(long chainId, SyncCallback cb)
    {
        syncCallback = cb;
        currentNetwork = findDefaultNetworkInteract.getNetworkInfo(chainId);

        startWalletUpdate();
    }

    private void startWalletUpdate()
    {
        walletBalances.clear();
        progress.postValue(true);


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

        if (ethereumNetworkRepository.isMainNetSelected())
        {
            startBalanceUpdateTimer(items);
            startFullWalletSync(items);
        }
        else
        {
            for (Wallet w : items)
            {
                if (w.type == WalletType.WATCH) continue;
                syncFromDBOnly(w, true);
            }
        }
    }

    private void startFullWalletSync(Wallet[] items)
    {
        //set all wallets as syncing
        walletSync = Observable.fromArray(items)
                .filter(wallet -> wallet.type != WalletType.WATCH) //no need to sync watch wallets
                .forEach(wallet -> startWalletSync(wallet)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::sendUnsyncedValue, e -> { }).isDisposed());
    }

    private void syncFromDBOnly(Wallet wallet, boolean complete)
    {
        tokenRepository.getTotalValue(wallet.address.toLowerCase(), EthereumNetworkBase.getAllMainNetworks())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value ->
                        {
                            if (complete)
                            {
                                syncCallback.syncCompleted(wallet.address.toLowerCase(), value);
                            }
                            else
                            {
                                syncCallback.syncStarted(wallet.address.toLowerCase(), value);
                            }
                        }).isDisposed();
    }

    private void sendUnsyncedValue(Wallet wallet)
    {
        TokensService service = walletServices.get(wallet.address.toLowerCase());
        if (service != null)
        {
            service.getFiatValuePair()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(value ->
                            syncCallback.syncStarted(service.getCurrentAddress().toLowerCase(), value)).isDisposed();
        }
    }

    private Single<Wallet> startWalletSync(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            TokensService svs = new TokensService(ethereumNetworkRepository, tokenRepository, tickerService, null, null);
            svs.setCurrentAddress(wallet.address.toLowerCase());
            svs.startUpdateCycle();
            svs.setCompletionCallback(this, 2);
            walletServices.put(wallet.address.toLowerCase(), svs);
            return wallet;
        });
    }

    @Override
    public void syncComplete(final TokensService service, int syncCount)
    {
        if (syncCount == 2)
        {
            service.setCompletionCallback(this, 1);
        }
        else
        {
            service.stopUpdateCycle();
            walletServices.remove(service.getCurrentAddress().toLowerCase());
        }

        if (syncCallback == null) return;

        //get value:
        service.getFiatValuePair()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(value -> {
                    if (syncCount == 1) { syncCallback.syncCompleted(service.getCurrentAddress().toLowerCase(), value); }
                        else { syncCallback.syncUpdate(service.getCurrentAddress().toLowerCase(), value); }
                }).isDisposed();
    }

    public void swipeRefreshWallets()
    {
        //check for updates
        //check names first
        ensWrappingCheck = fetchWalletsInteract.fetch().toObservable()
                .flatMap(Observable::fromArray)
                .forEach(wallet -> ensCheck = ensResolver.reverseResolveEns(wallet.address)
                                                    .onErrorReturnItem(wallet.ENSname != null ? wallet.ENSname : "")
                        .map(ensName -> { wallet.ENSname = ensName; return wallet;})
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(w -> fetchWalletsInteract.updateWalletData(w, () -> { }), this::onError));

        updateWallets();
    }

    public void fetchWallets()
    {
        progress.postValue(true);
        startWalletUpdate();
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
                .forEach(wallet -> walletBalanceUpdate = tokensService.getChainBalance(wallet.address.toLowerCase(), currentNetwork.chainId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(newBalance -> genericWalletInteract.updateBalanceIfRequired(wallet, newBalance), e -> { }));

        progress.postValue(false);
    }

    @Override
    public void onCleared()
    {
        super.onCleared();
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed()) balanceTimerDisposable.dispose();
        if (walletBalanceUpdate != null && !walletBalanceUpdate.isDisposed()) walletBalanceUpdate.dispose();
        if (ensCheck != null && !ensCheck.isDisposed()) ensCheck.dispose();
        if (ensWrappingCheck != null && !ensWrappingCheck.isDisposed()) ensWrappingCheck.dispose();
        if (walletSync != null && !walletSync.isDisposed()) walletSync.dispose();
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

    public void onDestroy()
    {
        for (TokensService svs : walletServices.values())
        {
            svs.stopUpdateCycle();
        }

        walletServices.clear();
    }
}
