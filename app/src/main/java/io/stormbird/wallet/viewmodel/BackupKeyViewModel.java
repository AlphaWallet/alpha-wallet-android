package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.text.TextUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.DeleteWalletInteract;
import io.stormbird.wallet.interact.ExportWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;

public class BackupKeyViewModel extends BaseViewModel {
    private final static String TAG = BackupKeyViewModel.class.getSimpleName();

    private final DeleteWalletInteract deleteWalletInteract;
    private final ExportWalletInteract exportWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;

    private final MutableLiveData<Wallet> saved = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();
    private final MutableLiveData<String> exportedStore = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> exportWalletError = new MutableLiveData<>();
    private final MutableLiveData<ErrorEnvelope> deleteWalletError = new MutableLiveData<>();

    public BackupKeyViewModel(
            DeleteWalletInteract deleteWalletInteract,
            ExportWalletInteract exportWalletInteract,
            FetchWalletsInteract fetchWalletsInteract) {
        this.deleteWalletInteract = deleteWalletInteract;
        this.exportWalletInteract = exportWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
    }

    public LiveData<ErrorEnvelope> exportWalletError() {
        return exportWalletError;
    }

    public LiveData<ErrorEnvelope> deleteWalletError() {
        return deleteWalletError;
    }

    public LiveData<Boolean> deleted() {
        return deleted;
    }

    public LiveData<Wallet> saved() {
        return saved;
    }

    public LiveData<String> exportedStore() {
        return exportedStore;
    }

    public void deleteWallet(Wallet wallet) {
        disposable = deleteWalletInteract
                .delete(wallet)
                .subscribe(this::onDelete, this::onDeleteWalletError);
    }

    private void onDeleteWalletError(Throwable throwable) {
        deleteWalletError.postValue(
                new ErrorEnvelope(C.ErrorCode.UNKNOWN, TextUtils.isEmpty(throwable.getLocalizedMessage())
                        ? throwable.getMessage() : throwable.getLocalizedMessage()));
    }

    private void onDelete(Wallet[] wallets) {
        deleted.postValue(true);
    }

    public void exportWallet(String walletAddr, String storePassword) {
        disposable = exportWalletInteract
                .export(new Wallet(walletAddr), storePassword)
                .subscribe(exportedStore::postValue, this::onExportWalletError);
    }

    private void onExportWalletError(Throwable throwable) {
        exportWalletError.postValue(
                new ErrorEnvelope(C.ErrorCode.UNKNOWN, TextUtils.isEmpty(throwable.getLocalizedMessage())
                        ? throwable.getMessage() : throwable.getLocalizedMessage()));
    }

    public void storeWallet(Wallet wallet) {
        disposable = fetchWalletsInteract.storeWallet(wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(saved::postValue, this::onError);
    }

    @Override
    protected void onError(Throwable throwable) {
        super.onError(throwable);
    }
}

