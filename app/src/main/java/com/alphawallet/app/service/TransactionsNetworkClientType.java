package com.alphawallet.app.service;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface TransactionsNetworkClientType {
    Observable<Transaction[]> fetchLastTransactions(NetworkInfo networkInfo, String tokenAddress, long lastBlock, String userAddress);
    Single<ContractType> checkConstructorArgs(NetworkInfo networkInfo, String address);
}
