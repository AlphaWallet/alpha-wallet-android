package com.alphawallet.app.service;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.tokens.Token;

import io.reactivex.Single;

public interface TransactionsNetworkClientType {
    Single<Transaction[]> storeNewTransactions(String walletAddress, NetworkInfo networkInfo, String tokenAddress, long lastBlock, long lastTxUpdate);
    void storeBlockRead(Token token, String walletAddress);
}
