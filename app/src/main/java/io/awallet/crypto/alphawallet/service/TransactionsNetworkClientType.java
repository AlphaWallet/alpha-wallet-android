package io.awallet.crypto.alphawallet.service;

import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionsCallback;
import io.awallet.crypto.alphawallet.entity.Wallet;

import io.reactivex.Observable;

public interface TransactionsNetworkClientType {
	Observable<Transaction[]> fetchTransactions(String forAddress);

    Observable<Transaction[]> fetchLastTransactions(Wallet wallet, Transaction lastTransaction);
    void fetchTransactions2(Wallet wallet, Transaction lastTransaction, TransactionsCallback txCallback);
}
