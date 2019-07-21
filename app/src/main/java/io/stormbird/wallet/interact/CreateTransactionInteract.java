package io.stormbird.wallet.interact;


import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TransactionRepositoryType;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class CreateTransactionInteract
{
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore)
    {
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair, int chainId)
    {
        //TODO: type of key
        return passwordStore.getPassword(wallet)
                .flatMap(password -> transactionRepository.getSignature(wallet, messagePair.message.getBytes(), password, chainId))
                .map(sig -> new SignaturePair(messagePair.selection, sig, messagePair.message));
    }

    public Single<byte[]> sign(Wallet wallet, byte[] message, int chainId)
    {
        //TODO: Type of key
        return passwordStore.getPassword(wallet)
                .flatMap(password -> transactionRepository.getSignature(wallet, message, password, chainId)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
    {
        //fgh

        return passwordStore.getPassword(from)
                .flatMap(password ->
                        transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, password, chainId)
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<String> create(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId)
    {
        //TODO: Branch depending on type of key
        return passwordStore.getPassword(from)
                .flatMap(password ->
                                 transactionRepository.createTransaction(from, gasPrice, gasLimit, data, password, chainId)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<TransactionData> createWithSig(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
    {
        //TODO: Branch depending on type of key
        return passwordStore.getPassword(from)
                .flatMap(password ->
                                 transactionRepository.createTransactionWithSig(from, to, subunitAmount, gasPrice, gasLimit, data, password, chainId)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<TransactionData> createWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId)
    {
        //TODO: Branch depending on type of key
        return passwordStore.getPassword(from)
                .flatMap(password ->
                                 transactionRepository.createTransactionWithSig(from, gasPrice, gasLimit, data, password, chainId)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread()));
    }
}