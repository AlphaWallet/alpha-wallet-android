package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsService;
import com.alphawallet.token.entity.ContractAddress;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

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

    /**
     * Check new tokens for any unknowns, then find the unknowns
     * @param rawTxList
     */
    public void checkTokens(RealmResults<RealmTransaction> rawTxList)
    {
        for (RealmTransaction tx : rawTxList)
        {
            if (tx.getError() != null && tx.getError().equals("0") &&
                    tx.getInput() != null && tx.getInput().length() > 2) //is this a successful contract transaction?
            {
                Token token = tokensService.getToken(tx.getChainId(), tx.getTo());
                if (token == null) tokensService.addUnknownTokenToCheck(new ContractAddress(tx.getChainId(), tx.getTo()));
            }
        }
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
