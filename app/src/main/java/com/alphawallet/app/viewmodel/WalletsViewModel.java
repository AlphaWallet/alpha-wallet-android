package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
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
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.util.AWEnsResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.repository.TokenRepository.getWeb3jService;

public class WalletsViewModel extends BaseViewModel
{
    private final static String TAG = WalletsViewModel.class.getSimpleName();

    private final SetDefaultWalletInteract setDefaultWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final KeyService keyService;
    private final GasService gasService;

    private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;
    private final TokensService tokensService;

    private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> noWalletsError = new MutableLiveData<>();
    private final MutableLiveData<Wallet> updateBalance = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> updateENSName = new MutableLiveData<>();

    private NetworkInfo currentNetwork;
    private Map<String, Wallet> walletBalances = new HashMap<>();
    private final ExecutorService executorService;
    private int walletUpdateCount;

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
            TokensService tokensService)
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

        executorService = Executors.newFixedThreadPool(10);
    }

    public LiveData<Wallet[]> wallets()
    {
        return wallets;
    }

    public LiveData<NetworkInfo> defaultNetwork()
    {
        return defaultNetwork;
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
                .subscribe(() -> onDefaultWalletChanged(wallet), this::onError);
    }

    private Wallet addWalletToMap(Wallet wallet) {
        if (wallet.address != null && !walletBalances.containsKey(wallet.address.toLowerCase())) walletBalances.put(wallet.address.toLowerCase(), wallet);
        return wallet;
    }

    public void findNetwork()
    {
        progress.postValue(true);
        ContractLocator override = EthereumNetworkRepository.getOverrideToken();
        NetworkInfo networkInfo  = findDefaultNetworkInteract.getNetworkInfo(override.chainId);
        defaultNetwork.postValue(networkInfo);
        currentNetwork = networkInfo;

        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWalletChanged,
                           error -> noWalletsError.postValue(true));
    }

    private void onWallets(Wallet[] items)
    {
        progress.postValue(false);

        for (Wallet w : items)
        {
            w.balanceSymbol = defaultNetwork.getValue().symbol;
            Wallet mapW = walletBalances.get(w.address);
            if (mapW != null)
            {
                w.balance = mapW.balance;
            }
        }
        wallets.postValue(items);
    }

    public void updateBalancesIfRequired(Wallet[] wallets)
    {
        if (walletBalances.size() == 0)
        {
            getWalletsBalance(wallets);
        }
    }

    private void onDefaultWalletChanged(Wallet wallet)
    {
        defaultWallet.postValue(wallet);

        //now load the current wallets from database
        disposable = fetchWalletsInteract
                .fetch()
                .subscribe(this::onWallets, this::onError);
    }

    public void swipeRefreshWallets()
    {
        walletBalances.clear();
        //check for updates
        //check names first
        disposable = fetchWalletsInteract.fetch().toObservable()
                .flatMap(Observable::fromArray)
                .flatMap(this::resolveEns)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateOnName, this::onError, () -> progress.postValue(false));
    }

    private void updateOnName(Wallet wallet)
    {
        if (wallet.ENSname != null && wallet.ENSname.length() > 0)
        {
            disposable = fetchWalletsInteract.updateWalletData(wallet)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(updateENSName::postValue, this::onError);
        }
    }

    private Observable<Wallet> resolveEns(Wallet wallet)
    {
        return Observable.fromCallable(() -> {
            AWEnsResolver resolver = new AWEnsResolver(getWeb3jService(EthereumNetworkRepository.MAINNET_ID), gasService);
            try
            {
                wallet.ENSname = resolver.reverseResolve(wallet.address);
                if (wallet.ENSname != null && wallet.ENSname.length() > 0)
                {
                    //check ENS name integrity - it must point to the wallet address
                    String resolveAddress = resolver.resolve(wallet.ENSname);
                    if (!resolveAddress.equalsIgnoreCase(wallet.address))
                    {
                        wallet.ENSname = null;
                    }
                }
            }
            catch (Exception e)
            {
                //ignore
            }
            return wallet;
        }).subscribeOn(Schedulers.from(executorService));
    }

    public void fetchWallets()
    {
        progress.postValue(true);
        findNetwork();
    }

    public void newWallet(Activity ctx, CreateWalletCallbackInterface createCallback)
    {
        keyService.createNewHDKey(ctx, createCallback);
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
                    fetchTokensInteract.fetchStoredToken(network, wallet, override.name) //fetch cached balance from this wallet's DB
                    .flatMap(tokenFromCache -> fetchTokensInteract.updateBalance(wallet.address, tokenFromCache)) //update balance
                    .subscribe(this::updateWallet, error -> onFetchError(wallet, network)).isDisposed();
                }, this::onError);
    }

    private void onFetchError(Wallet wallet, NetworkInfo networkInfo)
    {
        //leave wallet blank
        if (walletBalances.containsKey(wallet.address.toLowerCase()))
        {
            wallet.zeroWalletBalance(networkInfo);
            updateBalance.postValue(wallet);
            walletBalances.put(wallet.address.toLowerCase(), wallet);
        }
        storeWallets();
    }

    private void updateWallet(Token token)
    {
        if (walletBalances.containsKey(token.getWallet().toLowerCase()))
        {
            Wallet wallet = walletBalances.get(token.getWallet().toLowerCase());
            if (wallet != null)
            {
                if (wallet.setWalletBalance(token))
                {
                    //update wallet balance
                    updateBalance.postValue(wallet);
                }
            }
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
}
