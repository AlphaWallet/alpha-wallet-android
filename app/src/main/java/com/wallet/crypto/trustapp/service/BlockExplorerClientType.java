package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Transaction;

import io.reactivex.Observable;

public interface BlockExplorerClientType {
	Observable<Transaction[]> fetchTransactions(String forAddress);
}
