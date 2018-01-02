package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.entity.Transaction;

import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TransactionRepositoryType {
	Observable<Transaction[]> fetchTransaction(Wallet wallet);
	Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash);
	Single<String> createTransaction(Wallet from, String toAddress, String wei, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password);
}
