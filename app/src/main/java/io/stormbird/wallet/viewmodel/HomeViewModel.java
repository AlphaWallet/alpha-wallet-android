package io.stormbird.wallet.viewmodel;

import android.app.DownloadManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.ImportTokenRouter;
import io.stormbird.wallet.router.SettingsRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.HomeActivity;
import io.stormbird.wallet.util.LocaleUtils;

public class HomeViewModel extends BaseViewModel {
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<Transaction[]> transactions = new MutableLiveData<>();

    private final SettingsRouter settingsRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final ImportTokenRouter importTokenRouter;
    private final AddTokenRouter addTokenRouter;
    private final LocaleRepositoryType localeRepository;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<File> installIntent = new MutableLiveData<>();

    public static final String ALPHAWALLET_DIR = "AlphaWallet";
    public static final String ALPHAWALLET_FILE_URL = "https://awallet.io/apk";

    HomeViewModel(
            LocaleRepositoryType localeRepository,
            ImportTokenRouter importTokenRouter,
            ExternalBrowserRouter externalBrowserRouter,
            AddTokenRouter addTokenRouter,
            SettingsRouter settingsRouter,
            AssetDefinitionService assetDefinitionService) {
        this.settingsRouter = settingsRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.importTokenRouter = importTokenRouter;
        this.addTokenRouter = addTokenRouter;
        this.localeRepository = localeRepository;
        this.assetDefinitionService = assetDefinitionService;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public LiveData<NetworkInfo> defaultNetwork() {
        return defaultNetwork;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public LiveData<Transaction[]> transactions() {
        return transactions;
    }

    public LiveData<File> installIntent()
    {
        return installIntent;
    }

    public void prepare() {
        progress.postValue(false);
    }

    public void showSettings(Context context)
    {
        settingsRouter.open(context);
    }

    public void showImportLink(Context context, String importData)
    {
        importTokenRouter.open(context, importData);
    }

    public void openDeposit(Context context, Uri uri) {
        externalBrowserRouter.open(context, uri);
    }

    public void showAddToken(Context context) {
        addTokenRouter.open(context);
    }

    public void setLocale(HomeActivity activity)
    {
        //get the current locale
        String currentLocale = localeRepository.getDefaultLocale();
        LocaleUtils.setLocale(activity, currentLocale);
    }

    public void loadExternalXMLContracts()
    {
        assetDefinitionService.checkExternalDirectoryAndLoad();
    }

    public void downloadAndInstall(String build, Context ctx)
    {
        createDirectory();
        downloadAPK(build, ctx);
    }

    private void createDirectory()
    {
        //create XML repository directory
        File directory = new File(
                Environment.getExternalStorageDirectory()
                        + File.separator + ALPHAWALLET_DIR);

        if (!directory.exists())
        {
            directory.mkdir();
        }
    }

    private void downloadAPK(String version, Context ctx)
    {
        String destination = Environment.getExternalStorageDirectory()
                + File.separator + ALPHAWALLET_DIR ;

        File testFile = new File(destination, "AlphaWallet-" + version + ".apk");
        if (testFile.exists())
        {
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
        BroadcastReceiver onComplete = new BroadcastReceiver()
        {
            public void onReceive(Context ctxt, Intent intent)
            {
                installIntent.postValue(testFile);
                ctx.unregisterReceiver(this);
            }
        };

        ctx.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}
