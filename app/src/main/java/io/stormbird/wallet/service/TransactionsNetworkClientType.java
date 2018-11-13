package io.stormbird.wallet.service;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletUpdate;

public interface TransactionsNetworkClientType {
    Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, Wallet wallet, long lastBlock, String userAddress);
    Single<WalletUpdate> scanENSTransactionsForWalletNames(Wallet[] wallets, long lastBlock);

    Single<Integer> checkConstructorArgs(NetworkInfo networkInfo, String address);
}
