package com.alphawallet.app.interact;


import com.alphawallet.app.entity.MessagePair;
import com.alphawallet.app.entity.SignaturePair;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.cryptokeys.SignatureReturnType;
import com.alphawallet.app.repository.TransactionRepositoryType;

import java.math.BigInteger;

import com.alphawallet.token.entity.Signable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CreateTransactionInteract
{
    private final TransactionRepositoryType transactionRepository;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository)
    {
        this.transactionRepository = transactionRepository;
    }

    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair, int chainId)
    {
        return transactionRepository.getSignature(wallet, messagePair, chainId)
                .map(sig -> new SignaturePair(messagePair.selection, sig.signature, messagePair.message));
    }

    public Single<SignatureFromKey> sign(Wallet wallet, Signable message, int chainId)
    {
        return transactionRepository.getSignature(wallet, message, chainId)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
    {
        return transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, chainId)
                                             .subscribeOn(Schedulers.computation())
                                             .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TransactionData> createWithSig(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
    {
        return transactionRepository.createTransactionWithSig(from, to, subunitAmount, gasPrice, gasLimit, data, chainId)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<TransactionData> createWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId)
    {
        return transactionRepository.createTransactionWithSig(from, gasPrice, gasLimit, data, chainId)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> resend(Wallet from, BigInteger nonce, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
    {
        return transactionRepository.resendTransaction(from, to, subunitAmount, nonce, gasPrice, gasLimit, data, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void removeOverridenTransaction(Wallet wallet, String oldTxHash)
    {
        transactionRepository.removeOldTransaction(wallet, oldTxHash);
    }
}