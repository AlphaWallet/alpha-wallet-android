package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTransaction;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionsCallback;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TransactionRepositoryType;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FetchTransactionsInteract {

    private final TransactionRepositoryType transactionRepository;

    public FetchTransactionsInteract(TransactionRepositoryType transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Observable<Transaction[]> fetchCached(NetworkInfo network, Wallet wallet) {
        return transactionRepository
                .fetchCachedTransactions(network, wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Transaction[]> fetch(Wallet wallet) {
        return transactionRepository
                .fetchTransaction(wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<TokenTransaction[]> fetch(Wallet wallet, Token t) {
        return transactionRepository
                .fetchTokenTransaction(wallet, t)
                .subscribeOn(Schedulers.io());
    }

    public Observable<Transaction[]> fetchNetworkTransactions(Wallet wallet, long lastBlock) {
        return transactionRepository
                .fetchNetworkTransaction(wallet, lastBlock)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Transaction[]> fetchInternalTransactions(Wallet wallet, String feemaster) {
        return transactionRepository
                .fetchInternalTransactionsNetwork(wallet, feemaster)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
    {
        return transactionRepository.storeTransactions(networkInfo, wallet, txList);
    }

    public Observable<Transaction[]> storeTransactionsObservable(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
    {
        return transactionRepository.storeTransactions(networkInfo, wallet, txList).toObservable();
    }

//    public void fetchTx2(Wallet wallet, TransactionsCallback txCallback) {
//        transactionRepository
//                .fetchTransaction2(wallet, txCallback);
//    }
}
