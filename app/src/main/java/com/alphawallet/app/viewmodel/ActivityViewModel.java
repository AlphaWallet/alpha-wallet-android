package com.alphawallet.app.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsService;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

/**
 * Created by JB on 26/06/2020.
 */
public class ActivityViewModel extends BaseViewModel
{
    private final int TRANSACTION_FETCH_LIMIT = 500;

    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<ActivityMeta[]> activityItems = new MutableLiveData<>();

    private final GenericWalletInteract genericWalletInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final TransactionsService transactionsService;

    @Nullable
    private Disposable queryUnknownTokensDisposable;

    public LiveData<Wallet> defaultWallet() {
        return wallet;
    }
    public LiveData<ActivityMeta[]> activityItems() { return activityItems; }

    ActivityViewModel(
            GenericWalletInteract genericWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            TransactionsService transactionsService) {
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.transactionsService = transactionsService;
    }

    public void prepare()
    {
        //load the activity meta list
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet defaultWallet)
    {
        wallet.postValue(defaultWallet);
        disposable =
                fetchTransactionsInteract.fetchTransactionMetas(defaultWallet, tokensService.getNetworkFilters(), 0, TRANSACTION_FETCH_LIMIT)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onActivityMetas, this::onError);
    }

    private void onActivityMetas(ActivityMeta[] metas)
    {
        activityItems.postValue(metas);
        disposable =
                fetchTransactionsInteract.fetchEventMetas(wallet.getValue(), tokensService.getNetworkFilters())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(activityItems::postValue, this::onError);
    }

    public void fetchMoreTransactions(long startTime)
    {
        List<Integer> currentChains = tokensService.getNetworkFilters();
        disposable = Observable.fromIterable(currentChains)
                .flatMap(chainId -> transactionsService.fetchAndStoreTransactions(chainId, startTime).toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(activityItems::postValue, this::onError);

//        disposable = transactionsService.fetchAndStoreTransactions(1, startTime)
//                          .subscribeOn(Schedulers.io())
//                          .observeOn(AndroidSchedulers.mainThread())
//                          .subscribe(activityItems::postValue, this::onError);

//                fetchTransactionsInteract.fetchTransactionMetas(wallet.getValue(), tokensService.getNetworkFilters(), startTime, TRANSACTION_FETCH_LIMIT)
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(activityItems::postValue, this::onError);
    }

    public void onDestroy()
    {
        if (queryUnknownTokensDisposable != null && !queryUnknownTokensDisposable.isDisposed())
            queryUnknownTokensDisposable.dispose();

        queryUnknownTokensDisposable = null;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public FetchTransactionsInteract provideTransactionsInteract()
    {
        return fetchTransactionsInteract;
    }

    public Realm getRealmInstance()
    {
        return fetchTransactionsInteract.getRealmInstance(wallet.getValue());
    }

    public Transaction getTransaction(String hash)
    {
        return fetchTransactionsInteract.fetchCached(wallet.getValue().address, hash);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
