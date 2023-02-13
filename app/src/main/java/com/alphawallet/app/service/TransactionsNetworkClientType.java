package com.alphawallet.app.service;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.transactionAPI.TransferFetchType;

import io.reactivex.Single;

public interface TransactionsNetworkClientType {
    Single<Transaction[]> storeNewTransactions(TokensService svs, NetworkInfo networkInfo, String tokenAddress, long lastBlock);
    Single<TransactionMeta[]> fetchMoreTransactions(TokensService svs, NetworkInfo network, long lastTxTime);
    Single<Integer> readTransfers(String currentAddress, NetworkInfo networkByChain, TokensService tokensService, TransferFetchType tfType);
    void checkRequiresAuxReset(String walletAddr);
}
