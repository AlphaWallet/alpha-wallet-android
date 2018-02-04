package com.wallet.crypto.alphawallet.interact;


import com.wallet.crypto.alphawallet.entity.MessagePair;
import com.wallet.crypto.alphawallet.entity.SignaturePair;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;

import java.math.BigInteger;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CreateTransactionInteract {
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    //TODO: refactor with full reactive chaining (ie remove the blocking get)
    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair) {
        return Single.fromCallable(() -> {
            Single<byte[]> sig = passwordStore.getPassword(wallet)
                    .flatMap(password ->
                            transactionRepository.getSignature(wallet, messagePair.message, password)
                                    .observeOn(AndroidSchedulers.mainThread()));

            SignaturePair sigPair = new SignaturePair(messagePair.selection, sig.blockingGet());
            return sigPair;
        });
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data) {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                        transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, password)
                .observeOn(AndroidSchedulers.mainThread()));
    }
}
