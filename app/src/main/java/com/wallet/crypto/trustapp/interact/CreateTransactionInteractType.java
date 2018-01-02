package com.wallet.crypto.trustapp.interact;


import com.wallet.crypto.trustapp.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Single;

public interface CreateTransactionInteractType {

    public Single<String> create(Wallet from, String to, String wei, BigInteger gasPrice, BigInteger gasLimit, String password);
}
