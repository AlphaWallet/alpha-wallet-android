package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.FileData;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.stormbird.wallet.C.DOWNLOAD_READY;
import static io.stormbird.wallet.viewmodel.HomeViewModel.ALPHAWALLET_FILE_URL;

public class SplashViewModel extends ViewModel {
    private final FetchWalletsInteract fetchWalletsInteract;
    private final CreateWalletInteract createWalletInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;

    private MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private MutableLiveData<Wallet> createWallet = new MutableLiveData<>();

    SplashViewModel(FetchWalletsInteract fetchWalletsInteract,
                    CreateWalletInteract createWalletInteract,
                    PreferenceRepositoryType preferenceRepository,
                    LocaleRepositoryType localeRepository) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.createWalletInteract = createWalletInteract;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
    }

    public void setLocale(Context context) {
        localeRepository.setDefaultLocale(context, preferenceRepository.getDefaultLocale());
    }

    public void fetchWallets()
    {
        fetchWalletsInteract
                .fetch()
                .subscribe(wallets::postValue, this::onError);
    }

    //on wallet error ensure execution still continues and splash screen terminates
    private void onError(Throwable throwable) {
        wallets.postValue(new Wallet[0]);
    }

    public LiveData<Wallet[]> wallets() {
        return wallets;
    }
    public LiveData<Wallet> createWallet() {
        return createWallet;
    }

    public void createNewWallet(Activity ctx)
    {
        //create a new wallet for the user
        createWalletInteract
                .create(ctx)
                .subscribe(account -> {
                    fetchWallets();
                    createWallet.postValue(account);
                }, this::onError);
    }

    public void checkVersionUpdate(Context ctx, long updateTime)
    {
        if (!isPlayStoreInstalled(ctx))
        {
            //check the current install version string against the current version on the alphawallet page
            //current version number as string
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
            int asks = pref.getInt("update_asks", 0);
            if (updateTime == 0 || asks == 2) // if user cancels update twice stop asking them until the next release
            {
                pref.edit().putInt("update_asks", 0).apply();
                pref.edit().putLong("install_time", System.currentTimeMillis()).apply();
            }
            else
            {
                checkWebsiteAPKFileData(updateTime, ctx);
            }
        }
    }

    private boolean isPlayStoreInstalled(Context ctx)
    {
        // A list with valid installers package name
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));

        // The package name of the app that has installed your app
        final String installer = ctx.getPackageManager().getInstallerPackageName(ctx.getPackageName());

        // true if your app has been downloaded from Play Store
        return installer != null && validInstallers.contains(installer);
    }

    private String stripFilename(String name)
    {
        int index = name.lastIndexOf(".apk");
        if (index > 0)
        {
            name = name.substring(0, index);
        }
        index = name.lastIndexOf("-");
        if (index > 0)
        {
            name = name.substring(index+1);
        }
        return name;
    }

    private void checkWebsiteAPKFileData(long currentInstallDate, final Context baseContext)
    {
        getFileDataFromURL(ALPHAWALLET_FILE_URL)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> onUpdate(result, currentInstallDate, baseContext), this::onError).isDisposed();
    }

    private Single<FileData> getFileDataFromURL(final String location)
    {
        return Single.fromCallable(() -> tryFileData(location));
    }

    private FileData tryFileData(String stepLocation)
    {
        FileData fileData = null;
        HttpURLConnection connection = null;
        String redirectLocation = null;
        try
        {
            URL url = new URL(stepLocation);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setInstanceFollowRedirects(false);
            redirectLocation = connection.getHeaderField("Location");
            if (redirectLocation == null)
            {
                fileData = new FileData();
                fileData.fileDate = connection.getLastModified();
                fileData.fileName = stepLocation.substring(stepLocation.lastIndexOf('/') + 1, stepLocation.length());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (connection != null) connection.disconnect();
        }

        if (fileData != null)
        {
            return fileData;
        }
        else if (redirectLocation != null)
        {
            return tryFileData(redirectLocation);
        }
        else
        {
            return new FileData();
        }
    }

    private void onUpdate(FileData data, long currentInstallDate, Context baseContext)
    {
        //if needs update can we spring open a dialogue box from here?
        if (data.fileDate > currentInstallDate)
        {
            String newVersion = stripFilename(data.fileName);
            Intent intent = new Intent(DOWNLOAD_READY);
            intent.putExtra("Version", newVersion);
            baseContext.sendBroadcast(intent);
        }
    }
}
