package io.stormbird.wallet.interact;


import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.viewmodel.BaseViewModel;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class CreateTransactionInteract
{
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore)
    {
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair)
    {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> transactionRepository.getSignature(wallet, messagePair.message.getBytes(), password))
                .map(sig -> new SignaturePair(messagePair.selection, sig, messagePair.message));
    }

    public Single<byte[]> sign(Wallet wallet, byte[] message)
    {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> transactionRepository.getSignature(wallet, message, password)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data)
    {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                        transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, password)
                                .subscribeOn(Schedulers.computation())
                                .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<String> create(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data)
    {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                                 transactionRepository.createTransaction(from, gasPrice, gasLimit, data, password)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<TransactionData> createWithSig(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data)
    {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                                 transactionRepository.createTransactionWithSig(from, to, subunitAmount, gasPrice, gasLimit, data, password)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<TransactionData> createWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data)
    {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                                 transactionRepository.createTransactionWithSig(from, gasPrice, gasLimit, data, password)
                                         .subscribeOn(Schedulers.computation())
                                         .observeOn(AndroidSchedulers.mainThread()));
    }
}