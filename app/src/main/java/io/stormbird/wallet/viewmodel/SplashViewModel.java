package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;

import static io.stormbird.wallet.C.DEFAULT_NETWORK;
import static io.stormbird.wallet.C.DOWNLOAD_READY;
import static io.stormbird.wallet.C.HARD_CODED_KEY;
import static io.stormbird.wallet.C.OVERRIDE_DEFAULT_NETWORK;
import static io.stormbird.wallet.C.PRE_LOADED_KEY;
import static io.stormbird.wallet.viewmodel.HomeViewModel.ALPHAWALLET_FILE_URL;

public class SplashViewModel extends ViewModel {
    private final FetchWalletsInteract fetchWalletsInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final ImportWalletInteract importWalletInteract;
    private final AddTokenInteract addTokenInteract;
    private final CreateWalletInteract createWalletInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;

    private MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
    private MutableLiveData<Wallet> createWallet = new MutableLiveData<>();

    SplashViewModel(FetchWalletsInteract fetchWalletsInteract,
                    EthereumNetworkRepositoryType networkRepository,
                    ImportWalletInteract importWalletInteract,
                    AddTokenInteract addTokenInteract,
                    CreateWalletInteract createWalletInteract,
                    PreferenceRepositoryType preferenceRepository,
                    LocaleRepositoryType localeRepository) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.networkRepository = networkRepository;
        this.importWalletInteract = importWalletInteract;
        this.addTokenInteract = addTokenInteract;
        this.createWalletInteract = createWalletInteract;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
    }

    public void setLocale(Context context) {
        localeRepository.setDefaultLocale(context, preferenceRepository.getDefaultLocale());
    }

    /**
     * This is slightly confusing because we have to guard against race condition so here is code execution order:
     * 1. check if there is a default network override
     * 2. check if there's a hard coded key - if there is then go to 5, otherwise drop to 3.
     * 3. check if there's a hard coded contract - if so jump to 6 otherwise drop to 4.
     * 4. fetch wallets then jump to FINISH
     * 5. - load hard coded key then jump back to 3. (check hard coded contract).
     * 6. - load hard coded contract then jump back to 4. (load wallets)
     * FINISH - push wallet message so SplashAcitivity now continues with execution of ::onWallets
     */
    public void startOverridesChain() {
        if (OVERRIDE_DEFAULT_NETWORK && !preferenceRepository.getDefaultNetworkSet()) {
            NetworkInfo[] networks = networkRepository.getAvailableNetworkList();
            for (NetworkInfo networkInfo : networks) {
                if (networkInfo.name.equals(DEFAULT_NETWORK)) {
                    networkRepository.setDefaultNetworkInfo(networkInfo);
                    preferenceRepository.setDefaultNetworkSet();
                    break;
                }
            }
        }

        checkHardCodedKey();
    }

    //2. Check hardcoded key
    private void checkHardCodedKey()
    {
        //Chain events to eliminate race condition
        //first check if hard coded key is present, if it is then call it and chain contract check
        if (HARD_CODED_KEY) {
            addHardKey(PRE_LOADED_KEY);
        }
        else
        {
            fetchWallets();
        }
    }

    //4. fetch wallets
    private void fetchWallets()
    {
        fetchWalletsInteract
                .fetch(null)
                .subscribe(wallets::postValue, this::onError);
    }

    //5. add hardcoded key then always perform check hard coded contrats
    private void addHardKey(String key) {
        importWalletInteract
                .importPrivateKey(key)
                .subscribe(this::keyAdded, this::onKeyError);
    }

    private void keyAdded(Wallet wallet)
    {
        //success
        System.out.println("Imported wallet at addr: " + wallet.address);

        //continue chain
        fetchWallets();
    }

    //6. add hard coded contract
    private void addContract(String address, String symbol, int decimals, String name) {
        TokenInfo tokenInfo = getTokenInfo(address, symbol, decimals, name, true);
        addTokenInteract
                .add(tokenInfo)
                .subscribe(this::fetchWallets, this::onContractError); //directly call fetch wallets if successful
    }

    private void fetchWallets(Token token)
    {
        fetchWallets();
    }

    //on wallet error ensure execution still continues and splash screen terminates
    private void onError(Throwable throwable) {
        wallets.postValue(new Wallet[0]);
    }

    //on key error ensure contract check continues
    private void onKeyError(Throwable throwable) {
        fetchWallets();
    }

    //on contract error ensure we still call wallet fetch
    private void onContractError(Throwable throwable) {
        fetchWallets();
    }

    public LiveData<Wallet[]> wallets() {
        return wallets;
    }
    public LiveData<Wallet> createWallet() {
        return createWallet;
    }

    private TokenInfo getTokenInfo(String address, String symbol, int decimals, String name, boolean isStormBird)
    {
        TokenInfo tokenInfo = new TokenInfo(address, name, symbol, decimals, true, isStormBird);
        return tokenInfo;
    }

    public void createNewWallet()
    {
        //create a new wallet for the user
        createWalletInteract
                .create()
                .subscribe(account -> {
                    fetchWallets();
                    createWallet.postValue(account);
                }, this::onError);
    }

    public void checkVersionUpdate(Context ctx)
    {
        if (!isPlayStoreInstalled(ctx))
        {
            //check the current install version string against the current version on the alphawallet page
            //current version number as string
            try
            {
                PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                String version = pInfo.versionName;
                String versionName = BuildConfig.VERSION_NAME;
                //convert number into actual number
                int buildValue = getBuildValueFromString(versionName);
                //get remote filename on server
                checkWebsiteAPKFilename(buildValue, ctx);
            }
            catch (PackageManager.NameNotFoundException e)
            {
                e.printStackTrace();
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

    private int getBuildValueFromString(String versionName)
    {
        versionName = stripFilename(versionName);

        String[] values = versionName.split("[.]");
        int version = 0;
        try
        {
            if (values.length >= 3)
            {
                version = Integer.valueOf(values[0]) * 100 * 100 + Integer.valueOf(values[1]) * 100 + Integer.valueOf(values[2]);
            }
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
        }

        return version;
    }

    private void checkWebsiteAPKFilename(int buildValue, final Context baseContext)
    {
        Disposable d = getFileNameFromURL(ALPHAWALLET_FILE_URL).toObservable()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> onUpdate(result, buildValue, baseContext), this::onError);
    }

    private void onUpdate(String name, int buildValue, Context baseContext)
    {
        //if needs update can we spring open a dialogue box from here?
        int newValue = getBuildValueFromString(name);
        if (newValue > buildValue)
        {
            String newVersion = stripFilename(name);
            Intent intent = new Intent(DOWNLOAD_READY);
            intent.putExtra("Version", newVersion);
            baseContext.sendBroadcast(intent);
        }
    }

    private Single<String> getFileNameFromURL(final String location)
    {
        return Single.fromCallable(() -> {
            HttpURLConnection connection = null;
            String stepLocation = location;
            for (;;) //crawl through the URL linkage until we get the base filename
            {
                URL url = new URL(stepLocation);
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                String redirectLocation = connection.getHeaderField("Location");
                if (redirectLocation == null) break;
                stepLocation = redirectLocation;
                connection.disconnect();
            }
            connection.disconnect();
            return stepLocation.substring(stepLocation.lastIndexOf('/') + 1, stepLocation.length());
        });
    }
}
