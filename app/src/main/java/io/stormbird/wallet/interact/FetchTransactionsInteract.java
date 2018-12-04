package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.TransactionRepositoryType;

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

    public Observable<Transaction[]> fetchNetworkTransactions(Wallet wallet, long lastBlock, String userAddress) {
        return transactionRepository
                .fetchNetworkTransaction(wallet, lastBlock, userAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
    {
        return transactionRepository.storeTransactions(networkInfo, wallet, txList);
    }

    public Single<Integer> queryInterfaceSpec(Token token)
    {
        return transactionRepository.queryInterfaceSpec(token)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());
    }

    public Transaction fetchCached(String walletAddress, String hash)
    {
        return transactionRepository.fetchCachedTransaction(walletAddress, hash);
    }
}
