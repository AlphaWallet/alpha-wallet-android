package com.langitwallet.app.service;

import com.langitwallet.app.entity.NetworkInfo;
import com.langitwallet.app.entity.Transaction;
import com.langitwallet.app.entity.TransactionMeta;
import com.langitwallet.app.entity.transactionAPI.TransferFetchType;
import com.langitwallet.app.entity.transactions.TransferEvent;

import java.util.List;
import java.util.Map;

import io.reactivex.Single;

public interface TransactionsNetworkClientType
{
    Single<Transaction[]> storeNewTransactions(TokensService svs, NetworkInfo networkInfo, String tokenAddress, long lastBlock);

    Single<TransactionMeta[]> fetchMoreTransactions(TokensService svs, NetworkInfo network, long lastTxTime);

    Single<Map<String, List<TransferEvent>>> readTransfers(String currentAddress, NetworkInfo networkByChain, TokensService tokensService, TransferFetchType tfType);

    void checkRequiresAuxReset(String walletAddr);
}
