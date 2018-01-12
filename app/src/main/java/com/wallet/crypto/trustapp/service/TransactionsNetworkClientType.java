package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Observable;

public interface TransactionsNetworkClientType {
	Observable<Transaction[]> fetchTransactions(String forAddress);

    Observable<Transaction[]> fetchLastTransactions(Wallet wallet, Transaction lastTransaction);
}
