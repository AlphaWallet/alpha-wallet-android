package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.ServiceErrorException;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.service.HDKeyService;
import io.stormbird.wallet.ui.widget.OnSetWatchWalletListener;

public class ImportWalletViewModel extends BaseViewModel implements OnSetWatchWalletListener
{
    private final ImportWalletInteract importWalletInteract;
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> badSeed = new MutableLiveData<>();

    ImportWalletViewModel(ImportWalletInteract importWalletInteract) {
        this.importWalletInteract = importWalletInteract;
    }

    public void onKeystore(String keystoreDetails, String newPassword, HDKeyService.AuthenticationLevel level) {
        progress.postValue(true);
        String[] split = keystoreDetails.split("__");
        String keystore = split[0];
        String password = split[1];
        importWalletInteract
                .importKeystore(keystore, password, newPassword)
                .flatMap(wallet -> importWalletInteract.storeKeystoreWallet(wallet, level))
                .subscribe(this::onWallet, this::onError).isDisposed();
    }

    public void onPrivateKey(String privateKey, String newPassword, HDKeyService.AuthenticationLevel level) {
        progress.postValue(true);
        importWalletInteract
                .importPrivateKey(privateKey, newPassword)
                .flatMap(wallet -> importWalletInteract.storeKeystoreWallet(wallet, level))
                .subscribe(this::onWallet, this::onError).isDisposed();
    }

    @Override
    public void onWatchWallet(String address)
    {
        //user just asked for a watch wallet
        disposable = importWalletInteract.storeWatchWallet(address)
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

    public void onSeed(String walletAddress, HDKeyService.AuthenticationLevel level)
    {
        if (walletAddress == null)
        {
            System.out.println("ERROR");
            badSeed.postValue(true);
        }
        else
        {
            //begin key storage process
            disposable = importWalletInteract.storeHDWallet(walletAddress, level)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(wallet::postValue, this::onError); //signal to UI wallet import complete
        }
    }
}
