package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
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

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

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
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        networkInfo = findDefaultNetworkInteract.getNetworkInfo(EthereumNetworkRepository.MAINNET_ID); //always show mainnet eth in wallet page
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
        //can we repopulate values from map?
        if (walletBalances.size() > 0)
        {
            for (Wallet w : items)
            {
                Wallet mapW = walletBalances.get(w.address);
                if (mapW != null) w.balance = mapW.balance;
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
            //updateENSName.postValue(wallet);
            disposable = fetchWalletsInteract.updateWalletData(wallet)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(updateENSName::postValue, this::onError);
        }
    }

    private Observable<Wallet> resolveEns(Wallet wallet)
    {
        return Observable.fromCallable(() -> {
            AWEnsResolver resolver = new AWEnsResolver(getService(EthereumNetworkRepository.MAINNET_ID), gasService);
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

    private Web3j getService(int chainId)
    {
        OkHttpClient okClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
        NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(chainId);
        return Web3j.build(new HttpService(network.rpcServerUrl, okClient, false));
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
        ContractResult override = EthereumNetworkRepository.getOverrideToken();
        NetworkInfo    network  = findDefaultNetworkInteract.getNetworkInfo(override.chainId); //findDefaultNetworkInteract.getNetworkInfo(EthereumNetworkRepository.MAINNET_ID);

        disposable = fetchWalletList(wallets)
                .flatMapIterable(wallet -> wallet) //iterate through each wallet
                .map(this::addWalletToMap)
                .flatMap(wallet -> fetchTokensInteract.fetchBaseCurrencyBalance(network, override, wallet, tokensService)) //fetch wallet balance
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateWallet, this::onError, this::updateBalances);
    }

    private void updateWallet(Token token)
    {
        if (walletBalances.containsKey(token.getWallet().toLowerCase()))
        {
            Wallet wallet = walletBalances.get(token.getWallet().toLowerCase());
            if (wallet != null)
            {
                wallet.setWalletBalance(token);
                //update wallet balance
                updateBalance.postValue(wallet);
            }
        }
    }

    private void updateBalances()
    {
        progress.postValue(false);
        if (currentNetwork.isMainNetwork)
        {
            Wallet[] walletsFromUpdate = walletBalances.values().toArray(new Wallet[0]);
            storeWallets(walletsFromUpdate);
        }
    }

    private void storeWallets(Wallet[] wallets)
    {
        //write wallets to DB
        disposable = fetchWalletsInteract.storeWallets(wallets)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onStored, this::onError);
    }

    private void onStored(Wallet[] wallets)
    {
        Log.d(TAG, "Stored " + wallets.length + " Wallets");
    }

    private Observable<List<Wallet>> fetchWalletList(Wallet[] wallets)
    {
        return Observable.fromCallable(() -> new ArrayList<>(Arrays.asList(wallets)));
    }

    private void onCreateWalletError(Throwable throwable)
    {
        //Crashlytics.logException(throwable);
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
}
