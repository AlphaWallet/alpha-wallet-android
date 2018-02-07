package com.wallet.crypto.alphawallet.service;

import android.content.Context;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 7/02/2018.
 */

public class MarketQueueService
{
    private final long MARKET_INTERVAL = 10*60; // 10 minutes

    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    private Disposable marketQueueProcessing;
    private Context context;

    public MarketQueueService(Context ctx, OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository,
                              PasswordStore passwordStore) {
        this.context = ctx;
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    public void setMarketQueue(Disposable disposable)
    {
        marketQueueProcessing = disposable;
    }

    public Disposable getMarketQueue()
    {
        return marketQueueProcessing;
    }

    //TODO: handle completion of transaction formation
    public void processMarketTrades(TradeInstance[] trades)
    {
        for (TradeInstance t : trades) {
            System.out.println("SIG: " + t.getStringSig());
        }
    }

    //sign a trade transaction
    public Single<TradeInstance> sign(Wallet wallet, String password, TradeInstance t) {
        return transactionRepository.getSignature(wallet, t.getTradeData(), password)
                .map(sig -> new TradeInstance(t, sig));
    }

    public Single<TradeInstance[]> tradesInnerLoop(Wallet wallet, String password, BigInteger price, short[] tickets, Ticket ticket) {
        return Single.fromCallable(() ->
        {
            final int TRADE_AMOUNT = 100;
            TradeInstance[] trades = new TradeInstance[TRADE_AMOUNT];

            //initial expiry 10 minutes from now
            long expiry = System.currentTimeMillis() / 1000L + MARKET_INTERVAL;
            //TODO: replace this with a computation observable something like this:
//            Flowable.range(0, TRADE_AMOUNT)
//                    .observeOn(Schedulers.computation())
//                    .map(v -> getTradeMessageAndSignature...)
//                    .blockingSubscribe(this::addTradeSequence, this::onError, this::onAllTransactions);

            for (int i = 0; i < TRADE_AMOUNT; i++)
            {
                BigInteger expiryTimestamp = BigInteger.valueOf(expiry + (i * MARKET_INTERVAL));
                trades[i] = (getTradeMessageAndSignature(wallet, password, price, expiryTimestamp, tickets, ticket)
                        .blockingGet());
                float upd = ((float)i/TRADE_AMOUNT)*100.0f;
                BaseViewModel.onQueueUpdate((int)upd);
            }
            return trades;
        });
    }

    private Single<TradeInstance[]> getTradeMessages(Wallet wallet, BigInteger price, short[] tickets, Ticket ticket) {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> tradesInnerLoop(wallet, password, price, tickets, ticket));
    }

    private Single<TradeInstance> getTradeMessageAndSignature(Wallet wallet, String password, BigInteger price, BigInteger expiryTimestamp, short[] tickets, Ticket ticket) {
        return encodeMessageForTrade(price, expiryTimestamp, tickets, ticket)
                .flatMap(newTrade -> sign(wallet, password, newTrade));
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
            message.put(contract);
            ShortBuffer shortBuffer = message.slice().asShortBuffer();
            shortBuffer.put(tickets);
            //return message.array();
            return new TradeInstance(price, expiryTimestamp, tickets, ticket, message.array());
        });
    }

    public Observable<TradeInstance[]> getTradeInstances(Wallet wallet, BigInteger price, short[] tickets, Ticket ticket) {
        return getTradeMessages(wallet, price, tickets, ticket).toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

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

    public void createMarketOrders(Wallet wallet, BigInteger price, short[] ticketIDs, Ticket ticket)
    {
        marketQueueProcessing = getTradeInstances(wallet, price, ticketIDs, ticket)
                .subscribe(this::processMarketTrades, this::onError, this::onAllTransactions);
    }

    private void onError(Throwable error) {
        //something went wrong
    }

    public void onAllTransactions()
    {
        System.out.println("go2");
    }
}
