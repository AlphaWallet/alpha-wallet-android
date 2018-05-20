package io.stormbird.wallet.service;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionsCallback;
import io.stormbird.wallet.entity.Wallet;

import io.reactivex.Observable;

public interface TransactionsNetworkClientType {
	Observable<Transaction[]> fetchTransactions(String forAddress);
    Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, Wallet wallet, long lastBlock);
    Observable<Transaction[]> fetchContractTransactions(String address, String feemaster);
}
