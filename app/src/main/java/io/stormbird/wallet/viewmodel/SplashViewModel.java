package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

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
import static io.stormbird.wallet.C.HARD_CODED_KEY;
import static io.stormbird.wallet.C.OVERRIDE_DEFAULT_NETWORK;
import static io.stormbird.wallet.C.PRE_LOADED_KEY;

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
}
