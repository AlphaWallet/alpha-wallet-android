package com.wallet.crypto.trustapp.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.CreateWalletInteract;
import com.wallet.crypto.trustapp.interact.DeleteWalletInteract;
import com.wallet.crypto.trustapp.interact.ExportWalletInteract;
import com.wallet.crypto.trustapp.interact.FetchWalletsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SetDefaultWalletInteract;
import com.wallet.crypto.trustapp.router.ImportWalletRouter;
import com.wallet.crypto.trustapp.router.TransactionsRouter;

import static com.wallet.crypto.trustapp.C.IMPORT_REQUEST_CODE;

public class WalletsViewModel extends BaseViewModel {

	private final CreateWalletInteract createWalletInteract;
	private final SetDefaultWalletInteract setDefaultWalletInteract;
	private final DeleteWalletInteract deleteWalletInteract;
	private final FetchWalletsInteract fetchWalletsInteract;
	private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final ExportWalletInteract exportWalletInteract;

	private final ImportWalletRouter importWalletRouter;
    private final TransactionsRouter transactionsRouter;

	private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
	private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
	private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
	private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
	private final MutableLiveData<String> exportedStore = new MutableLiveData<>();
	private final MutableLiveData<ErrorEnvelope> exportWalletError = new MutableLiveData<>();

    WalletsViewModel(
            CreateWalletInteract createWalletInteract,
            SetDefaultWalletInteract setDefaultWalletInteract,
            DeleteWalletInteract deleteWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ExportWalletInteract exportWalletInteract,
            ImportWalletRouter importWalletRouter,
            TransactionsRouter transactionsRouter) {
		this.createWalletInteract = createWalletInteract;
		this.setDefaultWalletInteract = setDefaultWalletInteract;
		this.deleteWalletInteract = deleteWalletInteract;
		this.fetchWalletsInteract = fetchWalletsInteract;
		this.findDefaultWalletInteract = findDefaultWalletInteract;
		this.importWalletRouter = importWalletRouter;
		this.exportWalletInteract = exportWalletInteract;
		this.transactionsRouter = transactionsRouter;

		fetchWallets();
	}

	public LiveData<Wallet[]> wallets() {
		return wallets;
	}

	public LiveData<Wallet> defaultWallet() {
		return defaultWallet;
	}

    public LiveData<Wallet> createdWallet() {
        return createdWallet;
    }

    public LiveData<String> exportedStore() {
        return exportedStore;
    }

	public void setDefaultWallet(Wallet wallet) {
		disposable = setDefaultWalletInteract
				.set(wallet)
				.subscribe(() -> onDefaultWalletChanged(wallet), this::onError);
	}

	public void deleteWallet(Wallet wallet) {
		disposable = deleteWalletInteract
				.delete(wallet)
				.subscribe(this::onFetchWallets, this::onError);
	}

	private void onFetchWallets(Wallet[] items) {
		progress.postValue(false);
		wallets.postValue(items);
		disposable = findDefaultWalletInteract
				.find()
				.subscribe(this::onDefaultWalletChanged, t -> {});
	}

	private void onDefaultWalletChanged(Wallet wallet) {
		progress.postValue(false);
		defaultWallet.postValue(wallet);
	}

	public void fetchWallets() {
		progress.postValue(true);
		disposable = fetchWalletsInteract
				.fetch()
				.subscribe(this::onFetchWallets, this::onError);
	}

	public void newWallet() {
		progress.setValue(true);
		createWalletInteract
				.create()
				.subscribe(account -> {
					fetchWallets();
					createdWallet.postValue(account);
				}, this::onCreateWalletError);
	}

    public void exportWallet(Wallet wallet, String storePassword) {
        exportWalletInteract
                .export(wallet, storePassword)
                .subscribe(exportedStore::postValue, this::onExportError);
    }

    private void onExportError(Throwable throwable) {
        Crashlytics.logException(throwable);
        exportWalletError.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, null));
    }

    private void onCreateWalletError(Throwable throwable) {
        Crashlytics.logException(throwable);
        createWalletError.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, null));
	}

	public void importWallet(Activity activity) {
		importWalletRouter.openForResult(activity, IMPORT_REQUEST_CODE);
	}

    public void showTransactions(Context context) {
        transactionsRouter.open(context, true);
    }
}
