package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.ImportWalletCallback;
import io.stormbird.wallet.entity.ServiceErrorException;
import io.stormbird.wallet.entity.SignAuthenticationCallback;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.ui.widget.OnSetWatchWalletListener;
import io.stormbird.wallet.util.AWEnsResolver;

import static io.stormbird.wallet.repository.EthereumNetworkRepository.MAINNET_ID;

public class ImportWalletViewModel extends BaseViewModel implements OnSetWatchWalletListener
{
    private final ImportWalletInteract importWalletInteract;
    private final KeyService keyService;
    private final AWEnsResolver ensResolver;

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> badSeed = new MutableLiveData<>();

    ImportWalletViewModel(ImportWalletInteract importWalletInteract, KeyService keyService, GasService gasService) {
        this.importWalletInteract = importWalletInteract;
        this.keyService = keyService;
        this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), gasService);
    }

    public void onKeystore(String keystore, String password, String newPassword, KeyService.AuthenticationLevel level) {
        progress.postValue(true);

        importWalletInteract
                .importKeystore(keystore, password, newPassword)
                .flatMap(wallet -> importWalletInteract.storeKeystoreWallet(wallet, level, ensResolver))
                .subscribe(this::onWallet, this::onError).isDisposed();
    }

    public void onPrivateKey(String privateKey, String newPassword, KeyService.AuthenticationLevel level) {
        progress.postValue(true);
        importWalletInteract
                .importPrivateKey(privateKey, newPassword)
                .flatMap(wallet -> importWalletInteract.storeKeystoreWallet(wallet, level, ensResolver))
                .subscribe(this::onWallet, this::onError).isDisposed();
    }

    @Override
    public void onWatchWallet(String address)
    {
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

    private void onWallet(Wallet wallet) {
        progress.postValue(false);
        this.wallet.postValue(wallet);
    }

    public void onError(Throwable throwable) {
        if (throwable.getCause() instanceof ServiceErrorException) {
            if (((ServiceErrorException) throwable.getCause()).code == C.ErrorCode.ALREADY_ADDED){
                error.postValue(new ErrorEnvelope(C.ErrorCode.ALREADY_ADDED, null));
            }
        } else {
            error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
        }
    }

    public void onSeed(String walletAddress, KeyService.AuthenticationLevel level)
    {
        if (walletAddress == null)
        {
            System.out.println("ERROR");
            badSeed.postValue(true);
        }
        else
        {
            //begin key storage process
            disposable = importWalletInteract.storeHDWallet(walletAddress, level, ensResolver)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(wallet::postValue, this::onError); //signal to UI wallet import complete
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
            Boolean isValid = false;
            try
            {
                ObjectMapper objectMapper = new ObjectMapper();
                WalletFile walletFile = objectMapper.readValue(keystore, WalletFile.class);
                ECKeyPair kp = org.web3j.crypto.Wallet.decrypt(password, walletFile);
                String address = Numeric.prependHexPrefix(Keys.getAddress(kp));
                if (address.equalsIgnoreCase(keystoreAddress)) isValid = true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return isValid;
        });
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }
}
