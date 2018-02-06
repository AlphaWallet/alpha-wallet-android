package com.wallet.crypto.alphawallet.interact;


import com.wallet.crypto.alphawallet.entity.MessagePair;
import com.wallet.crypto.alphawallet.entity.SignaturePair;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class CreateTransactionInteract {
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    public CreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    public Single<SignaturePair> sign(Wallet wallet, MessagePair messagePair) {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> transactionRepository.getSignature(wallet, messagePair.message.getBytes(), password))
                .map(sig -> new SignaturePair(messagePair.selection, sig));
    }

    //sign a trade transaction
    public Single<TradeInstance> sign(Wallet wallet, TradeInstance input) {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> transactionRepository.getSignature(wallet, input.getTradeData(), password)
                .map(sig -> input.addSignature(sig))
                                .observeOn(AndroidSchedulers.mainThread()));
    }

    public Single<TradeInstance> getTradeMessageAndSignature(Wallet wallet, BigInteger price, BigInteger expiryTimestamp, short[] tickets, Ticket ticket) {
        return encodeMessageForTrade(price, expiryTimestamp, tickets, ticket)
                .flatMap(tradeInstance -> sign(wallet, tradeInstance));
    }

    private Single<TradeInstance> encodeMessageForTrade(BigInteger price, BigInteger expiryTimestamp, short[] tickets, Ticket ticket) {
        return Single.fromCallable(() -> {
            byte[] priceInWei = price.toByteArray();
            byte[] expiry = expiryTimestamp.toByteArray();
            ByteBuffer message = ByteBuffer.allocate(96 + tickets.length * 2);
            byte[] leadingZeros = new byte[32 - priceInWei.length];
            message.put(leadingZeros);
            message.put(priceInWei);
            byte[] leadingZerosExpiry = new byte[32 - expiry.length];
            message.put(leadingZerosExpiry);
            message.put(expiry);
            //TODO maybe need to cast to bigint
            //BigInteger addr = new BigInteger(CONTRACT_ADDR.substring(2), 16);
            byte[] contract = hexStringToBytes("000000000000000000000000" + ticket.getAddress().substring(2));
            System.out.println("length of contract: " + contract.length);
            message.put(contract);
            ShortBuffer shortBuffer = message.slice().asShortBuffer();
            shortBuffer.put(tickets);
            return new TradeInstance(price, expiryTimestamp, tickets, ticket, message.array());
        });
    }

//    private byte[] encodeMessageForTrade(BigInteger price, BigInteger expiryTimestamp, short[] tickets, Ticket ticket) {
//        byte[] priceInWei = price.toByteArray();
//        byte[] expiry = expiryTimestamp.toByteArray();
//        ByteBuffer message = ByteBuffer.allocate(96 + tickets.length * 2);
//        byte[] leadingZeros = new byte[32 - priceInWei.length];
//        message.put(leadingZeros);
//        message.put(priceInWei);
//        byte[] leadingZerosExpiry = new byte[32 - expiry.length];
//        message.put(leadingZerosExpiry);
//        message.put(expiry);
//        //TODO maybe need to cast to bigint
//        //BigInteger addr = new BigInteger(CONTRACT_ADDR.substring(2), 16);
//        byte[] contract = hexStringToBytes("000000000000000000000000" + ticket.getAddress().substring(2));
//        System.out.println("length of contract: " + contract.length);
//        message.put(contract);
//        ShortBuffer shortBuffer = message.slice().asShortBuffer();
//        shortBuffer.put(tickets);
//
//        return message.array();
//    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data) {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                        transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, password)
                .observeOn(AndroidSchedulers.mainThread()));
    }

    private byte[] hexStringToBytes(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
