package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportWalletRouter;

import java.util.*;

import static io.stormbird.wallet.C.IMPORT_REQUEST_CODE;

public class WalletsViewModel extends BaseViewModel
{
    private final static String TAG = WalletsViewModel.class.getSimpleName();

    private final CreateWalletInteract createWalletInteract;
    private final SetDefaultWalletInteract setDefaultWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;

    private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;

    private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
    private final MutableLiveData<Wallet> updateBalance = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> namedWallets = new MutableLiveData<>();
    private final MutableLiveData<Long> lastENSScanBlock = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    private NetworkInfo currentNetwork;
    private Map<String, Wallet> walletBalances = new HashMap<>();

    WalletsViewModel(
            CreateWalletInteract createWalletInteract,
            SetDefaultWalletInteract setDefaultWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract)
    {
        this.createWalletInteract = createWalletInteract;
        this.setDefaultWalletInteract = setDefaultWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.importWalletRouter = importWalletRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    public LiveData<Wallet[]> wallets()
    {
        return wallets;
    }

    public LiveData<Map<String, String>> namedWallets()
    {
        return namedWallets;
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

    public LiveData<Wallet> updateBalance()
    {
        return updateBalance;
    }

    public LiveData<Long> lastENSScanBlock()
    {
        return lastENSScanBlock;
    }

    public void setDefaultWallet(Wallet wallet)
    {
        disposable = setDefaultWalletInteract
                .set(wallet)
                .subscribe(() -> onDefaultWalletChanged(wallet), this::onError);
    }

    private Wallet addWalletToMap(Wallet wallet) {
        if (!walletBalances.containsKey(wallet.address)) walletBalances.put(wallet.address, wallet);
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

        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWalletChanged, t -> {
                });
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

    public void swipeRefreshWallets(long block)
    {
        walletBalances.clear();
        //check for updates
        //check names first
        disposable = fetchWalletsInteract.fetch()
                .flatMap(wallets -> fetchWalletsInteract.scanForNames(wallets, block))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNames, this::onError);
    }

    public void fetchWallets()
    {
        progress.postValue(true);
        findNetwork();
    }

    public void newWallet()
    {
        progress.setValue(true);
        createWalletInteract
                .create()
                .subscribe(account -> {
                    fetchWallets();
                    createdWallet.postValue(account);
                }, this::onCreateWalletError);
    }

    /**
     * Sequentially updates the wallet balances on the current network
     *
     * @param wallets
     */
    private void getWalletsBalance(Wallet[] wallets)
    {
        NetworkInfo network = findDefaultNetworkInteract.getNetworkInfo(EthereumNetworkRepository.MAINNET_ID);

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
            wallet.setWalletBalance(token.balance);
            //update wallet balance
            updateBalance.postValue(wallet);
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

    private void updateNames(WalletUpdate update)
    {
        //update names for wallets
        //got names?
        //preserve order
        for (Wallet w : wallets.getValue())
        {
            if (update.wallets.containsKey(w.address))
            {
                w.ENSname = update.wallets.get(w.address).ENSname;
            }
        }

        if (update.wallets.size() > 0)
        {
            wallets.postValue(wallets.getValue());
            storeWallets(wallets.getValue());
        }

        lastENSScanBlock.postValue(update.lastBlock);

        progress.postValue(false);
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
}
