package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.FileData;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.token.tools.TokenDefinition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;
import static com.alphawallet.app.viewmodel.HomeViewModel.ALPHAWALLET_DIR;
import static com.alphawallet.app.viewmodel.HomeViewModel.ALPHAWALLET_FILE_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class SplashViewModel extends ViewModel
{
    private static final String LEGACY_CERTIFICATE_DB = "CERTIFICATE_CACHE-db.realm";
    private static final String LEGACY_AUX_DB_PREFIX = "AuxData-";

    private final FetchWalletsInteract fetchWalletsInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final KeyService keyService;
    private final CurrencyRepositoryType currencyRepository;

    private MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private MutableLiveData<Wallet> createWallet = new MutableLiveData<>();

    SplashViewModel(FetchWalletsInteract fetchWalletsInteract,
                    PreferenceRepositoryType preferenceRepository,
                    LocaleRepositoryType localeRepository,
                    KeyService keyService,
                    AssetDefinitionService assetDefinitionService,
                    CurrencyRepositoryType currencyRepository) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
        this.keyService = keyService;
        this.assetDefinitionService = assetDefinitionService;
        this.currencyRepository = currencyRepository;
    }

    public void setLocale(Context context)
    {
        localeRepository.setLocale(context, localeRepository.getActiveLocale());
    }

    public void fetchWallets()
    {
        fetchWalletsInteract
                .fetch()
                .subscribe(wallets::postValue, this::onError)
                .isDisposed();
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

    public void createNewWallet(Activity ctx, CreateWalletCallbackInterface createCallback)
    {
        Completable.fromAction(() -> keyService.createNewHDKey(ctx, createCallback)) //create wallet on a computation thread to give UI a chance to complete all tasks
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
                .isDisposed();
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
            Intent intent = new Intent(C.DOWNLOAD_READY);
            intent.putExtra("Version", newVersion);
            baseContext.sendBroadcast(intent);
        }
    }

    public void StoreHDKey(String address, KeyService.AuthenticationLevel authLevel)
    {
        if (!address.equals(ZERO_ADDRESS))
        {
            Wallet wallet = new Wallet(address);
            wallet.type = WalletType.HDKEY;
            wallet.authLevel = authLevel;
            fetchWalletsInteract.storeWallet(wallet)
                    .map(w -> { preferenceRepository.setCurrentWalletAddress(w.address); return w; })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(account -> {
                        fetchWallets();
                    }, this::onError).isDisposed();
        }
        else
        {
            wallets.postValue(new Wallet[0]);
        }
    }


    private TokenDefinition parseFile(Context ctx, InputStream xmlInputStream) throws Exception
    {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = ctx.getResources().getConfiguration().getLocales().get(0);
        }
        else
        {
            locale = ctx.getResources().getConfiguration().locale;
        }

        return new TokenDefinition(
                xmlInputStream, locale, null);
    }

    public void importScriptFile(Context ctx, String importData, boolean appExternal)
    {
        try
        {
            InputStream iStream = ctx.getApplicationContext().getContentResolver().openInputStream(Uri.parse(importData));
            TokenDefinition td = parseFile(ctx, iStream);
            if (td.holdingToken == null || td.holdingToken.length() == 0) return; //tokenscript with no holding token is currently meaningless. Is this always the case?

            byte[] writeBuffer = new byte[32768];
            String newFileName = td.contracts.get(td.holdingToken).addresses.values().iterator().next().iterator().next();
            newFileName = newFileName + ".tsml";

            if (appExternal)
            {
                newFileName = ctx.getExternalFilesDir("") + File.separator + newFileName;
            }
            else
            {
                newFileName = Environment.getExternalStorageDirectory() + File.separator + ALPHAWALLET_DIR + File.separator + newFileName;
            }

            //Store
            iStream = ctx.getApplicationContext().getContentResolver().openInputStream(Uri.parse(importData));
            FileOutputStream fos = new FileOutputStream(newFileName);

            while (iStream.available() > 0)
            {
                fos.write(writeBuffer, 0, iStream.read(writeBuffer));
            }

            iStream.close();
            fos.flush();
            fos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean checkDebugDirectory()
    {
        File directory = new File(Environment.getExternalStorageDirectory()
                + File.separator + ALPHAWALLET_DIR);

        return directory.exists();
    }

    public void completeAuthentication(Operation taskCode)
    {
        keyService.completeAuthentication(taskCode);
    }

    public void failedAuthentication(Operation taskCode)
    {
        keyService.failedAuthentication(taskCode);
    }

    public void setCurrency() {
        currencyRepository.setDefaultCurrency(preferenceRepository.getDefaultCurrency());
    }

    public void cleanAuxData(Context ctx)
    {
        try
        {
            File[] files = ctx.getFilesDir().listFiles();
            for (File file : files)
            {
                String fileName = file.getName();
                if (fileName.startsWith(LEGACY_AUX_DB_PREFIX) || fileName.equals(LEGACY_CERTIFICATE_DB))
                {
                    deleteRecursive(file);
                }
            }
        }
        catch (Exception e)
        {
            //
        }
    }

    private void deleteRecursive(File fp)
    {
        if (fp.isDirectory())
        {
            File[] contents = fp.listFiles();
            if (contents != null)
            {
                for (File child : contents)
                    deleteRecursive(child);
            }
        }

        fp.delete();
    }

    public void setDefaultBrowser()
    {
        preferenceRepository.setActiveBrowserNetwork(MAINNET_ID);
    }
}
