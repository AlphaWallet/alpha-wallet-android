package com.alphawallet.app.repository;

import android.util.Pair;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.Signable;

import org.web3j.crypto.RawTransaction;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Single;
import io.realm.Realm;

public interface TransactionRepositoryType
{
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

    Single<Pair<SignatureFromKey, RawTransaction>> signTransaction(Wallet from, Web3Transaction w3Tx, long chainId);

    RawTransaction formatRawTransaction(Web3Transaction w3Tx, long nonce, long chainId);

    Single<String> sendTransaction(Wallet from, RawTransaction rtx, SignatureFromKey sigData, long chainId);

    Single<Transaction> fetchTransactionFromNode(String walletAddress, long chainId, String hash);
}
