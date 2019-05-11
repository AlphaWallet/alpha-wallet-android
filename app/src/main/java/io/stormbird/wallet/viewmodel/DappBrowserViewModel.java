package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.tools.Numeric;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.DApp;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.AddEditDappActivity;
import io.stormbird.wallet.ui.DappBrowserFragment;
import io.stormbird.wallet.ui.ImportTokenActivity;
import io.stormbird.wallet.ui.zxing.QRScanningActivity;
import io.stormbird.wallet.util.DappBrowserUtils;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.Web3Transaction;

import static io.stormbird.wallet.C.DAPP_DEFAULT_URL;
import static io.stormbird.wallet.C.IMPORT_STRING;
import static io.stormbird.wallet.ui.HomeActivity.DAPP_BARCODE_READER_REQUEST_CODE;
import static io.stormbird.wallet.ui.ImportTokenActivity.getUsdString;

public class DappBrowserViewModel extends BaseViewModel  {
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<Token> token = new MutableLiveData<>();

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    private double ethToUsd = 0;
    private ArrayList<String> bookmarks;

    DappBrowserViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            ConfirmationRouter confirmationRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
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

    public LiveData<Token> token() {
        return token;
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
        disposable = fetchTokensInteract.fetchEth(defaultNetwork.getValue(), wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateBalance, this::onError);
    }

    private void updateBalance(Token token) {
        this.token.postValue(token);
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
        disposable = createTransactionInteract.sign(defaultWallet.getValue(), signRequest, defaultNetwork.getValue().chainId)
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
                    .create(defaultWallet.getValue(), transaction.gasPrice, transaction.gasLimit, transaction.payload, defaultNetwork.getValue().chainId)
                    .subscribe(hash -> onCreateTransaction(hash, dAppFunction, url),
                               error -> dAppFunction.DAppError(error, errorMsg));

        }
        else
        {
            byte[] data = Numeric.hexStringToByteArray(transaction.payload);
            disposable = createTransactionInteract
                    .create(defaultWallet.getValue(), transaction.recipient.toString(), transaction.value, transaction.gasPrice, transaction.gasLimit, data, defaultNetwork.getValue().chainId)
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

    public void openConfirmation(Context context, Web3Transaction transaction, String requesterURL, NetworkInfo networkInfo)
    {
        String networkName = networkInfo.name;
        boolean mainNet = networkInfo.isMainNetwork;
        confirmationRouter.open(context, transaction, networkName, mainNet, requesterURL, networkInfo.chainId);
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

    public void addToMyDapps(Context context, String title, String url) {
        Intent intent = new Intent(context, AddEditDappActivity.class);
        DApp dapp = new DApp(title, url);
        intent.putExtra(AddEditDappActivity.KEY_DAPP, dapp);
        intent.putExtra(AddEditDappActivity.KEY_MODE, AddEditDappActivity.MODE_ADD);
        context.startActivity(intent);
    }

    public void share(Context context, String url) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, url);
            intent.setType("text/plain");
            context.startActivity(intent);
    }

    public void startScan(Activity activity) {
        Intent intent = new Intent(activity, QRScanningActivity.class);
        activity.startActivityForResult(intent, DAPP_BARCODE_READER_REQUEST_CODE);
    }

    public List<DApp> getDappsMasterList(Context context) {
        return DappBrowserUtils.getDappsList(context);
    }

    public NetworkInfo[] getNetworkList() {
        return ethereumNetworkRepository.getAvailableNetworkList();
    }

    public void setNetwork(int chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId, false);
        if (info != null)
        {
            ethereumNetworkRepository.setDefaultNetworkInfo(info);
            onDefaultNetwork(info);
        }
    }

    public void showImportLink(Context ctx, String qrCode)
    {
        Intent intent = new Intent(ctx, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(IMPORT_STRING, qrCode);
        ctx.startActivity(intent);
    }
}
