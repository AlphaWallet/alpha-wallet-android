package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;

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
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.CreateWalletCallbackInterface;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletType;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.interact.SetDefaultWalletInteract;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportWalletRouter;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.util.AWEnsResolver;
import okhttp3.OkHttpClient;

import static io.stormbird.wallet.C.IMPORT_REQUEST_CODE;
import static io.stormbird.wallet.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static io.stormbird.wallet.repository.EthereumNetworkRepository.MAINNET_ID;

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
            GasService gasService)
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
        networkInfo = findDefaultNetworkInteract.getNetworkInfo(MAINNET_ID); //always show mainnet eth in wallet page
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
            AWEnsResolver resolver = new AWEnsResolver(getService(MAINNET_ID), gasService);
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
        NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(MAINNET_ID);

        disposable = fetchWalletList(wallets)
                .flatMapIterable(wallet -> wallet) //iterate through each wallet
                .map(this::addWalletToMap)
                .flatMap(wallet -> fetchTokensInteract.fetchEth(network, wallet)) //fetch wallet balance
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateWallet, this::onError, this::updateBalances);
    }

    private void updateWallet(Token token)
    {
        if (walletBalances.containsKey(token.getAddress()))
        {
            Wallet wallet = walletBalances.get(token.getAddress());
            if (wallet != null)
            {
                wallet.setWalletBalance(token.balance);
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
        disposable = fetchWalletsInteract.storeWallets(wallets, currentNetwork.isMainNetwork)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onStored, this::onError);
    }

    private void onStored(Integer count)
    {
        Log.d(TAG, "Stored " + count + " Wallets");
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
        importWalletRouter.openForResult(activity, IMPORT_REQUEST_CODE);
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
        importWalletRouter.openWatchCreate(activity, IMPORT_REQUEST_CODE);
    }
}
