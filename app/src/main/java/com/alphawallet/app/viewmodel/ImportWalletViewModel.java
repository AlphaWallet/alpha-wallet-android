package com.alphawallet.app.viewmodel;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.ImportWalletCallback;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.ImportWalletInteract;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.ui.widget.OnSetWatchWalletListener;
import com.alphawallet.app.util.AWEnsResolver;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@HiltViewModel
public class ImportWalletViewModel extends BaseViewModel implements OnSetWatchWalletListener
{
    private final ImportWalletInteract importWalletInteract;
    private final KeyService keyService;
    private final AWEnsResolver ensResolver;
    private final AnalyticsServiceType analyticsService;

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> badSeed = new MutableLiveData<>();
    private final MutableLiveData<String> watchExists = new MutableLiveData<>();
    private String importWalletType = "";

    @Inject
    ImportWalletViewModel(ImportWalletInteract importWalletInteract, KeyService keyService,
                          AnalyticsServiceType analyticsService) {
        this.importWalletInteract = importWalletInteract;
        this.keyService = keyService;
        this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), keyService.getContext());
        this.analyticsService = analyticsService;
    }

    public void onKeystore(String keystore, String password, String newPassword, KeyService.AuthenticationLevel level) {
        importWalletType = C.AN_KEYSTORE;
        progress.postValue(true);

        importWalletInteract
                .importKeystore(keystore, password, newPassword)
                .flatMap(wallet -> importWalletInteract.storeKeystoreWallet(wallet, level, ensResolver))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWallet, this::onError).isDisposed();
    }

    public void onPrivateKey(String privateKey, String newPassword, KeyService.AuthenticationLevel level) {
        importWalletType = C.AN_PRIVATE_KEY;
        progress.postValue(true);
        importWalletInteract
                .importPrivateKey(privateKey, newPassword)
                .flatMap(wallet -> importWalletInteract.storeKeystoreWallet(wallet, level, ensResolver))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWallet, this::onError).isDisposed();
    }

    @Override
    public void onWatchWallet(String address)
    {
        //do we already have this as a wallet?
        if (keystoreExists(address))
        {
            watchExists.postValue(address);
            return;
        }

        progress.postValue(true);
        //user just asked for a watch wallet
        disposable = importWalletInteract.storeWatchWallet(address, ensResolver)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wallet::postValue, this::onError); //signal to UI wallet import complete
    }

    public LiveData<Wallet> wallet() {
        return wallet;
    }
    public LiveData<Boolean> badSeed() { return badSeed; }
    public LiveData<String> watchExists() { return watchExists; }

    private void onWallet(Wallet wallet) {
        progress.postValue(false);
        this.wallet.postValue(wallet);
        track();
    }

    public void onError(Throwable throwable) {
        error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
    }

    public void onSeed(String walletAddress, KeyService.AuthenticationLevel level)
    {
        importWalletType = C.AN_SEED_PHRASE;
        if (walletAddress == null)
        {
            progress.postValue(false);
            Timber.e("ERROR");
            badSeed.postValue(true);
        }
        else
        {
            progress.postValue(true);
            //begin key storage process
            disposable = importWalletInteract.storeHDWallet(walletAddress, level, ensResolver)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onWallet, this::onError); //signal to UI wallet import complete
        }
    }

//    public void getAuthorisation(String walletAddress, Activity activity, SignAuthenticationCallback callback)
//    {
//        keyService.getAuthenticationForSignature(walletAddress, activity, callback);
//    }

    public void importHDWallet(String seedPhrase, Activity activity, ImportWalletCallback callback)
    {
        keyService.importHDKey(seedPhrase, activity, callback);
    }

    public void importKeystoreWallet(String address, Activity activity, ImportWalletCallback callback)
    {
        keyService.createKeystorePassword(address, activity, callback);
    }

    public void importPrivateKeyWallet(String address, Activity activity, ImportWalletCallback callback)
    {
        keyService.createPrivateKeyPassword(address, activity, callback);
    }

    public boolean keystoreExists(String address)
    {
        return importWalletInteract.keyStoreExists(address);
    }

    public Single<Boolean> checkKeystorePassword(String keystore, String keystoreAddress, String password)
    {
        return Single.fromCallable(() -> {
            boolean isValid = false;
            ObjectMapper objectMapper = new ObjectMapper();
            WalletFile walletFile = objectMapper.readValue(keystore, WalletFile.class);
            ECKeyPair kp = org.web3j.crypto.Wallet.decrypt(password, walletFile);
            String address = Numeric.prependHexPrefix(Keys.getAddress(kp));
            if (address.equalsIgnoreCase(keystoreAddress)) isValid = true;
            return isValid;
        });
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public void completeAuthentication(Operation taskCode)
    {
        keyService.completeAuthentication(taskCode);
    }

    public void failedAuthentication(Operation taskCode)
    {
        keyService.failedAuthentication(taskCode);
    }

    public void track()
    {
        AnalyticsProperties analyticsProperties = new AnalyticsProperties();
        analyticsProperties.setWalletType(importWalletType);

        analyticsService.track(C.AN_IMPORT_WALLET, analyticsProperties);
    }
}
