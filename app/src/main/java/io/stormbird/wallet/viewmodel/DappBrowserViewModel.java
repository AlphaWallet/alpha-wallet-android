package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.Web3Transaction;

import java.math.BigInteger;
import java.util.ArrayList;

import static io.stormbird.wallet.C.DAPP_DEFAULT_URL;
import static io.stormbird.wallet.ui.ImportTokenActivity.getUsdString;

public class DappBrowserViewModel extends BaseViewModel {
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;

    private double ethToUsd = 0;
    private ArrayList<String> bookmarks;
    private GasSettings internalGasSettings = null;

    DappBrowserViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchGasSettingsInteract fetchGasSettingsInteract,
            FetchTokensInteract fetchTokensInteract,
            ConfirmationRouter confirmationRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return assetDefinitionService;
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }
    public MutableLiveData<GasSettings> gasSettings() {
        return gasSettings;
    }

    public String getUSDValue(double eth)
    {
        if (defaultNetwork.getValue().chainId == 1)
        {
            return "$" + getUsdString(ethToUsd * eth);
        }
        else
        {
            return "$0.00";
        }
    }

    public String getNetworkName()
    {
        if (defaultNetwork.getValue().chainId == 1)
        {
            return "";
        }
        else
        {
            return defaultNetwork.getValue().name;
        }
    }

    public void prepare(Context context) {
        progress.postValue(true);
        loadBookmarks(context);

        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void loadBookmarks(Context context)
    {
        bookmarks = getBrowserBookmarksFromPrefs(context);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        if (gasSettings.getValue() == null)
        {
            disposable = fetchGasSettingsInteract
                    .fetch(false)
                    .subscribe(this::onGasSettings, this::onError);
        }
    }

    private void onGasSettings(GasSettings gasSettings) {
        this.gasSettings.postValue(gasSettings);
        internalGasSettings = gasSettings;

        disposable = fetchTokensInteract.getEthereumTicker()
                .subscribeOn(Schedulers.io())
                .subscribe(this::onTicker, this::onError);
    }

    private void onTicker(Ticker ticker)
    {
        if (ticker != null && ticker.price_usd != null)
        {
            ethToUsd = Double.valueOf(ticker.price_usd);
        }
    }

    public Observable<Wallet> getWallet() {
        if (defaultWallet().getValue() != null) {
            return Observable.fromCallable(() -> defaultWallet().getValue());
        } else
            return findDefaultNetworkInteract.find()
                    .flatMap(networkInfo -> findDefaultWalletInteract
                            .find()).toObservable();
    }

    public void signMessage(byte[] signRequest, DAppFunction dAppFunction, Message<String> message) {
        disposable = createTransactionInteract.sign(defaultWallet.getValue(), signRequest)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig, message),
                           error -> dAppFunction.DAppError(error, message));
    }

    public void signTransaction(Web3Transaction transaction, DAppFunction dAppFunction, String url)
    {
        Message errorMsg = new Message<>("Error executing transaction", url, 0);

        BigInteger addr = Numeric.toBigInt(transaction.recipient.toString());

        if (addr.equals(BigInteger.ZERO)) //constructor
        {
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), transaction.gasPrice, transaction.gasLimit, transaction.payload)
                    .subscribe(hash -> onCreateTransaction(hash, dAppFunction, url),
                               error -> dAppFunction.DAppError(error, errorMsg));

        }
        else
        {
            byte[] data = Numeric.hexStringToByteArray(transaction.payload);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), transaction.recipient.toString(), transaction.value, transaction.gasPrice, transaction.gasLimit, data)
                    .subscribe(hash -> onCreateTransaction(hash, dAppFunction, url),
                               error -> dAppFunction.DAppError(error, errorMsg));
        }
    }

    private void onCreateTransaction(String s, DAppFunction dAppFunction, String url)
    {
        //pushed transaction
        Message<String> msg = new Message<>(s, url, 0);
        dAppFunction.DAppReturn(s.getBytes(), msg);
    }

    public void openConfirmation(Context context, Web3Transaction transaction, String requesterURL)
    {
        String networkName = defaultNetwork.getValue().name;
        boolean mainNet = defaultNetwork.getValue().isMainNetwork;
        confirmationRouter.open(context, transaction, networkName, mainNet, requesterURL);
    }

    private ArrayList<String> getBrowserBookmarksFromPrefs(Context context) {
        ArrayList<String> storedBookmarks;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(context).getString(C.DAPP_BROWSER_BOOKMARKS, "");
        if (!historyJson.isEmpty()) {
            storedBookmarks = new Gson().fromJson(historyJson, new TypeToken<ArrayList<String>>(){}.getType());
        } else {
            storedBookmarks = new ArrayList<>();
        }
        return storedBookmarks;
    }

    private void writeBookmarks(Context context, ArrayList<String> bookmarks)
    {
        String historyJson = new Gson().toJson(bookmarks);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(C.DAPP_BROWSER_BOOKMARKS, historyJson).apply();
    }

    public String getLastUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(C.DAPP_LASTURL_KEY, DAPP_DEFAULT_URL);
    }

    public void setLastUrl(Context context, String url) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(C.DAPP_LASTURL_KEY, url).apply();
    }

    public ArrayList<String> getBookmarks()
    {
        return bookmarks;
    }

    public void addBookmark(Context context, String url)
    {
        //add to list
        bookmarks.add(url);
        //store
        writeBookmarks(context, bookmarks);
    }

    public void removeBookmark(Context context, String url)
    {
        if (bookmarks.contains(url)) bookmarks.remove(url);
        writeBookmarks(context, bookmarks);
    }
}
