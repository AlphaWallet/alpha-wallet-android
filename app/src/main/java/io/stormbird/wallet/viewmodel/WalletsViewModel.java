package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.text.TextUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.DeleteWalletInteract;
import io.stormbird.wallet.interact.ExportWalletInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SetDefaultWalletInteract;
import io.stormbird.wallet.router.ImportWalletRouter;
import io.stormbird.wallet.router.HomeRouter;

import static io.stormbird.wallet.C.IMPORT_REQUEST_CODE;

public class WalletsViewModel extends BaseViewModel
{
	private final CreateWalletInteract createWalletInteract;
	private final SetDefaultWalletInteract setDefaultWalletInteract;
	private final DeleteWalletInteract deleteWalletInteract;
	private final FetchWalletsInteract fetchWalletsInteract;
	private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final ExportWalletInteract exportWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
	private final FindDefaultNetworkInteract findDefaultNetworkInteract;

	private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;

	private final MutableLiveData<Wallet[]> wallets = new MutableLiveData<>();
	private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
	private final MutableLiveData<Wallet> createdWallet = new MutableLiveData<>();
	private final MutableLiveData<ErrorEnvelope> createWalletError = new MutableLiveData<>();
	private final MutableLiveData<String> exportedStore = new MutableLiveData<>();
	private final MutableLiveData<ErrorEnvelope> exportWalletError = new MutableLiveData<>();
	private final MutableLiveData<ErrorEnvelope> deleteWalletError = new MutableLiveData<>();
	private final MutableLiveData<Map<String, BigDecimal>> updateBalance = new MutableLiveData<>();

	private NetworkInfo currentNetwork;
	private Map<String, BigDecimal> walletBalances = new HashMap<>();

    WalletsViewModel(
            CreateWalletInteract createWalletInteract,
            SetDefaultWalletInteract setDefaultWalletInteract,
            DeleteWalletInteract deleteWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ExportWalletInteract exportWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
			FetchTokensInteract fetchTokensInteract,
			FindDefaultNetworkInteract findDefaultNetworkInteract) {
		this.createWalletInteract = createWalletInteract;
		this.setDefaultWalletInteract = setDefaultWalletInteract;
		this.deleteWalletInteract = deleteWalletInteract;
		this.fetchWalletsInteract = fetchWalletsInteract;
		this.findDefaultWalletInteract = findDefaultWalletInteract;
		this.importWalletRouter = importWalletRouter;
		this.exportWalletInteract = exportWalletInteract;
		this.homeRouter = homeRouter;
		this.fetchTokensInteract = fetchTokensInteract;
		this.findDefaultNetworkInteract = findDefaultNetworkInteract;

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

    public LiveData<ErrorEnvelope> createWalletError() {
        return createWalletError;
    }

    public LiveData<String> exportedStore() {
        return exportedStore;
    }

    public LiveData<ErrorEnvelope> exportWalletError() {
        return exportWalletError;
    }

    public LiveData<ErrorEnvelope> deleteWalletError() {
        return deleteWalletError;
    }

    public LiveData<Map<String, BigDecimal>> updateBalance() { return updateBalance; }

	public void setDefaultWallet(Wallet wallet) {
		disposable = setDefaultWalletInteract
				.set(wallet)
				.subscribe(() -> onDefaultWalletChanged(wallet), this::onError);
	}

	public void deleteWallet(Wallet wallet) {
		disposable = deleteWalletInteract
				.delete(wallet)
				.subscribe(this::onFetchWallets, this::onDeleteWalletError);
	}

	private void findNetwork() {
		progress.postValue(true);
		disposable = findDefaultNetworkInteract
				.find()
				.subscribe(this::onDefaultNetwork, this::onError);
	}

	private void onDefaultNetwork(NetworkInfo networkInfo)
	{
		currentNetwork = networkInfo;
		//now load the current wallets
		disposable = fetchWalletsInteract
				.fetch(walletBalances)
				.subscribe(this::onFetchWallets, this::onError);
	}

	private void onFetchWallets(Wallet[] items) {
		progress.postValue(false);
		wallets.postValue(items);
		disposable = findDefaultWalletInteract
				.find()
				.subscribe(this::onDefaultWalletChanged, t -> {});

		getWalletsBalance(items);
	}

	private void onDefaultWalletChanged(Wallet wallet) {
		progress.postValue(false);
		defaultWallet.postValue(wallet);
	}

	public void fetchWallets() {
		progress.postValue(true);
		findNetwork();
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
                .subscribe(exportedStore::postValue, this::onExportWalletError);
    }

	/**
	 * Sequentially updates the wallet balances on the current network
	 * @param wallets
	 */
	private void getWalletsBalance(Wallet[] wallets)
	{
        walletBalances.clear();
		disposable = fetchWalletList(wallets)
				.flatMapIterable(wallet -> wallet) //iterate through each wallet
				.flatMap(wallet -> fetchTokensInteract.fetchEth(currentNetwork, wallet)) //fetch wallet balance
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::updateList, this::onError, this::updateBalances);
	}

    private void updateBalances()
    {
        updateBalance.postValue(walletBalances);
    }

    private void updateList(Token token)
	{
        walletBalances.put(token.getAddress(), token.balance);
	}

	private Observable<List<Wallet>> fetchWalletList(Wallet[] wallets)
	{
		return Observable.fromCallable(() -> {
			return new ArrayList<>(Arrays.asList(wallets));
		});
	}

    private void onExportWalletError(Throwable throwable) {
        //Crashlytics.logException(throwable);
        exportWalletError.postValue(
                new ErrorEnvelope(C.ErrorCode.UNKNOWN, TextUtils.isEmpty(throwable.getLocalizedMessage())
                                ? throwable.getMessage() : throwable.getLocalizedMessage()));
    }

    private void onDeleteWalletError(Throwable throwable) {
        //Crashlytics.logException(throwable);
        deleteWalletError.postValue(
                new ErrorEnvelope(C.ErrorCode.UNKNOWN, TextUtils.isEmpty(throwable.getLocalizedMessage())
                                ? throwable.getMessage() : throwable.getLocalizedMessage()));
    }

    private void onCreateWalletError(Throwable throwable) {
        //Crashlytics.logException(throwable);
        progress.postValue(false);
        createWalletError.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
	}

	public void importWallet(Activity activity) {
		importWalletRouter.openForResult(activity, IMPORT_REQUEST_CODE);
	}

    public void showHome(Context context) {
        homeRouter.open(context, true);
    }
}
