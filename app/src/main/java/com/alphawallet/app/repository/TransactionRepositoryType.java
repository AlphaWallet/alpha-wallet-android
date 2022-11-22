package com.alphawallet.app.repository;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.token.entity.Signable;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Single;
import io.realm.Realm;

public interface TransactionRepositoryType
{
    Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId);

    Single<TransactionData> create1559TransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasLimit, BigInteger maxFeePerGas, BigInteger maxPriorityFee, long nonce, byte[] data, long chainId);

    Single<TransactionData> getSignatureForTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, long nonce, byte[] data, long chainId);

    Single<SignatureFromKey> getSignature(Wallet wallet, Signable message);

    Single<byte[]> getSignatureFast(Wallet wallet, String password, byte[] message);

    Transaction fetchCachedTransaction(String walletAddr, String hash);

    long fetchTxCompletionTime(String walletAddr, String hash);

    Single<String> resendTransaction(Wallet from, String to, BigInteger subunitAmount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte[] data, long chainId);

    Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, List<Long> networkFilters, long fetchTime, int fetchLimit);

    Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, long chainId, String tokenAddress, int historyCount);

    Single<ActivityMeta[]> fetchEventMetas(Wallet wallet, List<Long> networkFilters);

    Realm getRealmInstance(Wallet wallet);

    RealmAuxData fetchCachedEvent(String walletAddress, String eventKey);

    void restartService();
}
