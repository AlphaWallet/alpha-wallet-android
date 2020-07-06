package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.app.DownloadManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.*;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.FragmentMessenger;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.LocaleUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.alphawallet.app.util.QRParser;
import com.alphawallet.app.widget.AWalletBottomNavigationView;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.tools.ParseMagicLink;
import com.alphawallet.app.R;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.AddTokenRouter;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.service.AssetDefinitionService;

import java.io.File;
import java.io.FilenameFilter;

import static org.web3j.crypto.WalletUtils.isValidAddress;

public class HomeViewModel extends BaseViewModel {
    private final String TAG = "HVM";
    public static final String ALPHAWALLET_DIR = "AlphaWallet";
    public static final String ALPHAWALLET_FILE_URL = "https://1x.alphawallet.com/dl/latest.apk";

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();
    private final MutableLiveData<String> backUpMessage = new MutableLiveData<>();

    private final PreferenceRepositoryType preferenceRepository;
    private final ImportTokenRouter importTokenRouter;
    private final AddTokenRouter addTokenRouter;
    private final LocaleRepositoryType localeRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final CurrencyRepositoryType currencyRepository;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final Context context;
    private final MyAddressRouter myAddressRouter;

    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;

    private final MutableLiveData<File> installIntent = new MutableLiveData<>();
    private final MutableLiveData<String> walletName = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    HomeViewModel(
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            ImportTokenRouter importTokenRouter,
            AddTokenRouter addTokenRouter,
            AssetDefinitionService assetDefinitionService,
            GenericWalletInteract genericWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            CurrencyRepositoryType currencyRepository,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            Context context,
            MyAddressRouter myAddressRouter) {
        this.preferenceRepository = preferenceRepository;
        this.importTokenRouter = importTokenRouter;
        this.addTokenRouter = addTokenRouter;
        this.localeRepository = localeRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.currencyRepository = currencyRepository;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.context = context;
        this.myAddressRouter = myAddressRouter;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }

    public LiveData<File> installIntent() {
        return installIntent;
    }

    public LiveData<String> backUpMessage() {
        return backUpMessage;
    }

    public void prepare() {
        progress.postValue(false);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    public void onClean()
    {

    }

    private void onDefaultWallet(final Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void showImportLink(Activity activity, String importData) {
        disposable = genericWalletInteract
                .find().toObservable()
                .filter(wallet -> checkWalletNotEqual(wallet, importData))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wallet -> importLink(wallet, activity, importData), this::onError);
    }

    private boolean checkWalletNotEqual(Wallet wallet, String importData) {
        boolean filterPass = false;

        try {
            if (cryptoFunctions == null) {
                cryptoFunctions = new CryptoFunctions();
            }
            if (parser == null) {
                parser = new ParseMagicLink(cryptoFunctions, EthereumNetworkRepository.extraChains());
            }

            MagicLinkData data = parser.parseUniversalLink(importData);
            String linkAddress = parser.getOwnerKey(data);

            if (isValidAddress(data.contractAddress)) {
                filterPass = !wallet.address.equals(linkAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filterPass;
    }

    private void importLink(Wallet wallet, Activity activity, String importData) {
        importTokenRouter.open(activity, importData);
    }

    public void showAddToken(Context context, String address) {
        addTokenRouter.open(context, address);
    }

    public void updateLocale(String newLocale, Context context)
    {
        localeRepository.setDefaultLocale(context, newLocale);
        //restart activity
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public void setLocale(HomeActivity activity) {
        //get the current locale
        String currentLocale = localeRepository.getDefaultLocale();
        LocaleUtils.setLocale(activity, currentLocale);
    }

    public void downloadAndInstall(String build, Context ctx) {
        createDirectory();
        downloadAPK(build, ctx);
    }

    private void createDirectory() {
        //create XML repository directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + ALPHAWALLET_DIR);

        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    private void downloadAPK(String version, Context ctx) {
        String destination = Environment.getExternalStorageDirectory()
                + File.separator + ALPHAWALLET_DIR;

        File testFile = new File(destination, "AlphaWallet-" + version + ".apk");
        if (testFile.exists()) {
            testFile.delete();
        }
        final Uri uri = Uri.parse("file://" + testFile.getPath());

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(ALPHAWALLET_FILE_URL));
        request.setDescription(ctx.getString(R.string.alphawallet_update) + " " + version);
        request.setTitle(ctx.getString(R.string.app_name));
        request.setDestinationUri(uri);
        final DownloadManager manager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);

        //set BroadcastReceiver to install app when .apk is downloaded
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                installIntent.postValue(testFile);
                ctx.unregisterReceiver(this);
            }
        };

        ctx.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public void getWalletName() {
        disposable = fetchWalletsInteract
                .getWallet(preferenceRepository.getCurrentWalletAddress())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWallet, this::onError);
    }

    private void onWallet(Wallet wallet) {
        if (TextUtils.isEmpty(wallet.ENSname))
        {
            walletName.postValue(wallet.name);
        }
        else
        {
            walletName.postValue(wallet.ENSname);
        }

        //check for ENS name (could have changed)
        new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), context)
                .resolveEnsName(wallet.address)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(walletName::postValue, this::onENSError).isDisposed();
    }

    public LiveData<String> walletName() {
        return walletName;
    }

    public void cleanDatabases(Context ctx)
    {
        File[] files = ctx.getFilesDir().listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File file, String name)
            {
                return name.matches("^0x\\S{40}-.+-db.real\\S+")
                        && !name.matches("^0x\\S{40}-721-db.real\\S+"); //match all deprecated databases
            }
        });

        for (File file : files)
        {
            //erase file
            deleteRecursive(file);
        }
    }

    public void checkIsBackedUp(String walletAddress)
    {
        genericWalletInteract.getWalletNeedsBackup(walletAddress)
                .subscribe(backUpMessage::postValue).isDisposed();
    }

    private void deleteRecursive(File fileDir)
    {
        if (fileDir.isDirectory()) {
            for (File child : fileDir.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileDir.delete();
    }

    public void cleanNewDatabases(Context ctx)
    {
        File[] files = ctx.getFilesDir().listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File file, String name)
            {
                return name.matches("^0x\\S{40}-db.real\\S+")
                    || name.matches("^0x\\S{40}-721-db.real\\S+"); //match all deprecated databases
            }
        });

        for (File file : files)
        {
            //erase file
            deleteRecursive(file);
        }
    }

    public boolean isFindWalletAddressDialogShown() {
        return preferenceRepository.isFindWalletAddressDialogShown();
    }

    public void setFindWalletAddressDialogShown(boolean isShown) {
        preferenceRepository.setFindWalletAddressDialogShown(isShown);
    }

    public String getDefaultCurrency(){
        return currencyRepository.getDefaultCurrency();
    }

    public void updateTickers()
    {
        ethereumNetworkRepository.refreshTickers();
    }

    private void onENSError(Throwable throwable)
    {
        if (BuildConfig.DEBUG) throwable.printStackTrace();
    }

    public void setErrorCallback(FragmentMessenger callback)
    {
        assetDefinitionService.setErrorCallback(callback);
    }

    public void handleQRCode(Activity activity, String qrCode)
    {
        try
        {
            if (qrCode == null) return;

            QRParser parser = QRParser.getInstance(EthereumNetworkBase.extraChains());
            QRResult qrResult = parser.parse(qrCode);

            switch (qrResult.type)
            {
                case ADDRESS:
                    showMyAddress(activity);
                    break;
                case PAYMENT:
                    showSend(activity, qrResult);
                    break;
                case TRANSFER:
                    showSend(activity, qrResult);
                    break;
                case FUNCTION_CALL:
                    //TODO: Handle via ConfirmationActivity
                    break;
                case URL:
                    ((HomeActivity)activity).onBrowserWithURL(qrCode);
                    break;
                case MAGIC_LINK:
                    showImportLink(activity, qrCode);
                    break;
                case OTHER:
                    qrCode = null;
                    break;
            }
        }
        catch (Exception e)
        {
            qrCode = null;
        }

        if(qrCode == null)
        {
            Toast.makeText(context, R.string.toast_invalid_code, Toast.LENGTH_SHORT).show();
        }
    }

    public void showSend(Activity ctx, QRResult result)
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

    public void showMyAddress(Activity activity)
    {
        myAddressRouter.open(activity, defaultWallet.getValue());
    }
}
