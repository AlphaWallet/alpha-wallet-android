package com.alphawallet.app;

import android.support.annotation.NonNull;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.BaseViewCallback;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.entity.TradeInstance;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.MarketQueueService;

import com.alphawallet.token.entity.Signable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ExecutorScheduler;
import io.reactivex.plugins.RxJavaPlugins;
import io.realm.Realm;

import com.alphawallet.token.entity.SalesOrderMalformed;

import static org.junit.Assert.assertEquals;

/**
 * Created by James on 21/03/2018.
 */

public class MarketOrderTest
{
    @Inject
    TransactionRepositoryType transactionRepository;

    private ECKeyPair testKey;
    private TradeInstance generatedTrade;

    @BeforeClass
    public static void setUpRxSchedulers() {
        Scheduler immediate = new Scheduler() {
            @Override
            public Worker createWorker()
            {
                return new ExecutorScheduler.ExecutorWorker(Runnable::run, false);
            }

            @Override
            public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
                // this prevents StackOverflowErrors when scheduling with a delay
                return super.scheduleDirect(run, 0, unit);
            }
        };

        RxJavaPlugins.setInitIoSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitComputationSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitNewThreadSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitSingleSchedulerHandler(scheduler -> immediate);
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> immediate);
    }

    private final MarketQueueService marketService;

    public MarketOrderTest()
    {
        //roll a new key
        testKey = ECKeyPair.create("Test Key".getBytes());

        transactionRepository = new TransactionRepositoryType() {

            @Override
            public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId) {
                return null;
            }

            @Override
            public Single<TransactionData> createTransactionWithSig(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
            {
                return null;
            }

            @Override
            public Single<TransactionData> createTransactionWithSig(Wallet from, BigInteger gasPrice, BigInteger gasLimit, String data, int chainId)
            {
                return null;
            }

            @Override
            public Single<SignatureFromKey> getSignature(Wallet wallet, Signable message, int chainId) {
                return null;
            }

            @Override
            public Single<byte[]> getSignatureFast(Wallet wallet, String password, byte[] message, int chainId) {
                return Single.fromCallable(() -> {
                    //sign using the local key
                    Sign.SignatureData sigData = Sign.signMessage(message, testKey);

                    byte[] sig = new byte[65];

                    try {
                        System.arraycopy(sigData.getR(), 0, sig, 0, 32);
                        System.arraycopy(sigData.getS(), 0, sig, 32, 32);
                        System.arraycopy(sigData.getV(), 0, sig, 64, 1);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        throw new SalesOrderMalformed("Signature shorter than expected 256");
                    }

                    return sig;
                });
            }

            @Override
            public Transaction fetchCachedTransaction(String walletAddr, String hash)
            {
                return null;
            }

            @Override
            public Single<String> resendTransaction(Wallet from, String to, BigInteger subunitAmount, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId)
            {
                return Single.fromCallable(() -> { return ""; });
            }

            @Override public void removeOldTransaction(Wallet wallet, String oldTxHash)
            {

            }

            @Override
            public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet,
                                                                      List<Integer> networkFilters, long fetchTime, int fetchLimit)
            {
                return null;
            }

            @Override
            public Single<ActivityMeta[]> fetchCachedTransactionMetas(Wallet wallet, int chainId,
                                                                      String tokenAddress,
                                                                      int historyCount)
            {
                return null;
            }

            @Override
            public Single<ActivityMeta[]> fetchEventMetas(Wallet wallet,
                                                          List<Integer> networkFilters)
            {
                return null;
            }

            @Override
            public Realm getRealmInstance(Wallet wallet)
            {
                return null;
            }

            @Override
            public RealmAuxData fetchCachedEvent(String walletAddress, String eventKey)
            {
                return null;
            }
        };

        marketService = new MarketQueueService(null, null, transactionRepository);
    }

    @Test
    public void testMarketQueue()
    {
        Wallet wallet = new Wallet("0x007bee82bdd9e866b2bd114780a47f2261c684e3");
        BigInteger price = BigInteger.valueOf(1234567);
        int[] tickets = { 12, 13, 14 };
        String contractAddress = "0x007bee82bdd9e866b2bd114780a47f2261c684e3";
        BigInteger firstTicketId = BigInteger.valueOf(1024);

        //1. generate the tradeInstance block and signature array
        marketService.setCallback(testCallback);
        marketService.getTradeInstances(wallet, price, tickets, contractAddress, firstTicketId, 1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processMarketTrades, this::onError, this::onAllTransactions);
    }

    private void onAllTransactions()
    {
        //TODO: Fix onAllTransactions
    }

    private BaseViewCallback testCallback = new BaseViewCallback() {

        @Override
        public void queueUpdate(int complete) {
            System.out.println(String.valueOf(complete));
        }

        @Override
        public void pushToast(String message) {
            System.out.println(message);
        }

        @Override
        public void showMarketQueueSuccessDialog(Integer resId) {
            System.out.println("Market Queue Success - string resource id: " + String.valueOf(resId));
        }

        @Override
        public void showMarketQueueErrorDialog(Integer resId) {
            System.out.println("Market Queue Error - string resource id: " + String.valueOf(resId));
        }
    };

    private void processMarketTrades(TradeInstance tradeInstance)
    {
        generatedTrade = tradeInstance;
    }

    private void onError(Throwable throwable) {
        assertEquals("test failed", throwable.getMessage());
    }
}
