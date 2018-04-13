package io.awallet.crypto.alphawallet.interact;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenTransaction;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionsCallback;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;

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

    public Observable<Transaction[]> fetchCached(Wallet wallet) {
        return transactionRepository
                .fetchCachedTransactions(wallet)
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

    public Observable<Transaction[]> fetchNetworkTransactions(Wallet wallet, Transaction lastTx) {
        return transactionRepository
                .fetchNetworkTransaction(wallet, lastTx)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
    {
        return transactionRepository.storeTransactions(networkInfo, wallet, txList);
    }

//    public void fetchTx2(Wallet wallet, TransactionsCallback txCallback) {
//        transactionRepository
//                .fetchTransaction2(wallet, txCallback);
//    }
}
