package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.os.TransactionTooLargeException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.ui.MyAddressActivity;
import com.alphawallet.app.ui.SendActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.DApp;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.GasSettings;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.ui.AddEditDappActivity;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.zxing.QRScanningActivity;
import com.alphawallet.app.util.DappBrowserUtils;
import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.web3.entity.Web3Transaction;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;

import org.web3j.abi.datatypes.Address;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.alphawallet.app.C.Key.WALLET;

public class DappBrowserViewModel extends BaseViewModel  {
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();
    private final MutableLiveData<Token> token = new MutableLiveData<>();

    private static final int BALANCE_CHECK_INTERVAL_SECONDS = 20;

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final GasService gasService;
    private final KeyService keyService;

    private ArrayList<String> bookmarks;
    @Nullable
    private Disposable balanceTimerDisposable;

    DappBrowserViewModel(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            ConfirmationRouter confirmationRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            GasService gasService,
            KeyService keyService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.gasService = gasService;
        this.keyService = keyService;
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
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(final Wallet wallet) {
        defaultWallet.setValue(wallet);
        //get the balance token

        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed()) balanceTimerDisposable.dispose();

        balanceTimerDisposable = Observable.interval(0, BALANCE_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS)
                    .doOnNext(l -> checkBalance(wallet)).subscribe();
    }

    private void checkBalance(final Wallet wallet)
    {
        Token blank = ethereumNetworkRepository.getBlankOverrideToken(defaultNetwork.getValue());
        String address = blank.getAddress().equals(Address.DEFAULT.toString()) ? wallet.address.toLowerCase() : blank.getAddress().toLowerCase();
        disposable = fetchTokensInteract.fetchStoredToken(defaultNetwork.getValue(), wallet, address)
                .flatMap(tokenFromCache -> fetchTokensInteract.updateBalance(wallet.address, tokenFromCache))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateBalance, error -> updateBalance(blank));
    }

    private void updateBalance(Token token) {
        this.token.postValue(token);
    }

    public Observable<Wallet> getWallet() {
        if (defaultWallet().getValue() != null) {
            return Observable.fromCallable(() -> defaultWallet().getValue());
        } else
            return findDefaultNetworkInteract.find()
                    .flatMap(networkInfo -> genericWalletInteract
                            .find()).toObservable();
    }

    public void signMessage(byte[] signRequest, DAppFunction dAppFunction, Message<String> message) {
        disposable = createTransactionInteract.sign(defaultWallet.getValue(), signRequest, defaultNetwork.getValue().chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig.signature, message),
                           error -> dAppFunction.DAppError(error, message));
    }

    public void openConfirmation(Activity context, Web3Transaction transaction, String requesterURL, NetworkInfo networkInfo) throws TransactionTooLargeException
    {
        confirmationRouter.open(context, transaction, networkInfo.name, requesterURL, networkInfo.chainId);
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
                .getString(C.DAPP_LASTURL_KEY, C.DAPP_DEFAULT_URL);
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
        activity.startActivityForResult(intent, HomeActivity.DAPP_BARCODE_READER_REQUEST_CODE);
    }

    public List<DApp> getDappsMasterList(Context context) {
        return DappBrowserUtils.getDappsList(context);
    }

    public NetworkInfo[] getNetworkList() {
        return ethereumNetworkRepository.getAvailableNetworkList();
    }

    public int getActiveFilterCount() {
        return ethereumNetworkRepository.getFilterNetworkList().size();
    }

    public void setNetwork(int chainId)
    {
        NetworkInfo info = ethereumNetworkRepository.getNetworkByChain(chainId);
        if (info != null)
        {
            ethereumNetworkRepository.setDefaultNetworkInfo(info);
            onDefaultNetwork(info);
            startGasPriceChecker();
        }
    }

    public void showImportLink(Context ctx, String qrCode)
    {
        Intent intent = new Intent(ctx, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, qrCode);
        ctx.startActivity(intent);
    }

    public void startGasPriceChecker()
    {
        if (defaultNetwork.getValue() != null)
        {
            gasService.startGasListener(defaultNetwork.getValue().chainId);
        }
    }

    public void stopGasPriceChecker()
    {
        gasService.stopGasListener();
    }

    public void getAuthorisation(Wallet wallet, Activity activity, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public void completeAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }

    public void failedAuthentication(Operation signData)
    {
        keyService.failedAuthentication(signData);
    }

    public void showSend(Context ctx, QRResult result)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        boolean sendingTokens = (result.getFunction() != null && result.getFunction().length() > 0);
        String address = defaultWallet.getValue().address;
        int decimals = 18;

        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getNetworkByChain(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, defaultWallet.getValue());
        intent.putExtra(C.EXTRA_TOKEN_ID, (Token)null);
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void showMyAddress(Context ctx)
    {
        Intent intent = new Intent(ctx, MyAddressActivity.class);
        intent.putExtra(WALLET, defaultWallet.getValue());
        ctx.startActivity(intent);
    }

    public void onDestroy()
    {
        if (balanceTimerDisposable != null && !balanceTimerDisposable.isDisposed()) balanceTimerDisposable.dispose();
    }
}
