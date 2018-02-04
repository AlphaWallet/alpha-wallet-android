package com.wallet.crypto.alphawallet.service;

import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;

import io.reactivex.Observable;

public interface TransactionsNetworkClientType {
	Observable<Transaction[]> fetchTransactions(String forAddress);

    Observable<Transaction[]> fetchLastTransactions(Wallet wallet, Transaction lastTransaction);
}
