package io.awallet.crypto.alphawallet;

import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.entity.BaseViewCallback;
import io.awallet.crypto.alphawallet.entity.MessageData;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.SalesOrderMalformed;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenTransaction;
import io.awallet.crypto.alphawallet.entity.TradeInstance;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.awallet.crypto.alphawallet.service.MarketQueueService;

import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.schedulers.ExecutorScheduler;
import io.reactivex.plugins.RxJavaPlugins;

import static org.junit.Assert.assertEquals;

/**
 * Created by James on 21/03/2018.
 */

public class MarketOrderTest
{
    @Inject
    TransactionRepositoryType transactionRepository;

    @Inject
    PasswordStore passwordStore;

    private ECKeyPair testKey;
    private TradeInstance generatedTrade;

    @BeforeClass
    public static void setUpRxSchedulers() {
        Scheduler immediate = new Scheduler() {
            @Override
            public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
                // this prevents StackOverflowErrors when scheduling with a delay
                return super.scheduleDirect(run, 0, unit);
            }

            @Override
            public Worker createWorker() {
                return new ExecutorScheduler.ExecutorWorker(Runnable::run);
            }
        };

        RxJavaPlugins.setInitIoSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitComputationSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitNewThreadSchedulerHandler(scheduler -> immediate);
        RxJavaPlugins.setInitSingleSchedulerHandler(scheduler -> immediate);
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> immediate);
    }

    private final MarketQueueService marketService;// = new MarketQueueService(null, null, );

    public MarketOrderTest()
    {
        //roll a new key
        testKey = ECKeyPair.create("Test Key".getBytes());

        //mock the interfaces we need
        passwordStore = new PasswordStore() {
            @Override
            public Single<String> getPassword(Wallet wallet) {
                return Single.fromCallable(() -> {
                    return "fake pwd";
                });
            }

            @Override
            public Completable setPassword(Wallet wallet, String password) {
                return null;
            }

            @Override
            public Single<String> generatePassword() {
                return null;
            }
        };

        transactionRepository = new TransactionRepositoryType() {

            @Override
            public Observable<Transaction[]> fetchCachedTransactions(Wallet wallet)
            {
                return null;
            }

            @Override
            public Observable<Transaction[]> fetchTransaction(Wallet wallet) {
                return null;
            }

            @Override
            public Observable<Transaction[]> fetchNetworkTransaction(Wallet wallet, Transaction lastBlock)
            {
                return null;
            }

            @Override
            public Observable<TokenTransaction[]> fetchTokenTransaction(Wallet wallet, Token token) {
                return null;
            }

            @Override
            public Maybe<Transaction> findTransaction(Wallet wallet, String transactionHash) {
                return null;
            }

            @Override
            public Single<String> createTransaction(Wallet from, String toAddress, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, String password) {
                return null;
            }

            @Override
            public Single<byte[]> getSignature(Wallet wallet, byte[] message, String password) {
                return null;
            }

            @Override
            public Single<byte[]> getSignatureFast(Wallet wallet, byte[] message, String password) {
                return Single.fromCallable(() -> {
                    //sign using the local key
                    Sign.SignatureData sigData = Sign.signMessage(message, testKey);

                    byte[] sig = new byte[65];

                    try {
                        System.arraycopy(sigData.getR(), 0, sig, 0, 32);
                        System.arraycopy(sigData.getS(), 0, sig, 32, 32);
                        sig[64] = (byte) (int) sigData.getV();
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        throw new SalesOrderMalformed("Signature shorter than expected 256");
                    }

                    return sig;
                });
            }

            @Override
            public void unlockAccount(Wallet signer, String signerPassword) throws Exception {

            }

            @Override
            public void lockAccount(Wallet signer, String signerPassword) throws Exception {

            }

            @Override
            public Single<Transaction[]> storeTransactions(NetworkInfo networkInfo, Wallet wallet, Transaction[] txList)
            {
                return null;
            }
        };

        marketService = new MarketQueueService(null, null, transactionRepository, passwordStore);
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
        marketService.getTradeInstances(wallet, price, tickets, contractAddress, firstTicketId)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(this::processMarketTrades, this::onError, this::onAllTransactions);
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

    private void onAllTransactions()
    {
        //now run through all the transactions, check the expiries are all correct and check the signature.
        try {
            MessageData firstEntryData = SalesOrder.readByteMessage(generatedTrade.getTradeBytes(), generatedTrade.getSignatureBytes(0), 3);

            long expiry = generatedTrade.expiry.longValue();
            String decimalVal = "" + firstEntryData.priceWei;
            BigInteger milliWei = Convert.fromWei(decimalVal, Convert.Unit.FINNEY).toBigInteger();
            double price = milliWei.doubleValue() / 1000.0;
            String testAddress = "0x" + Keys.getAddress(testKey.getPublicKey());
            String contractAddress = Numeric.toHexString(generatedTrade.contractAddress.toByteArray());

            for (byte[] sig : generatedTrade.getSignatures()) {
                byte[] sigStr64 = Base64.encode(sig);
                byte[] tradeBytes64 = Base64.encode(generatedTrade.getTradeBytes());
                //generate the sales order from the individual entry
                SalesOrder so = new SalesOrder(price, expiry, 1024, 3, contractAddress, new String(sigStr64),
                        new String(tradeBytes64));

                so.getOwnerKey();

                //check all the ec-recovered addresses
                assertEquals(testAddress, so.ownerAddress);

                //see if we can recover
                expiry += 10*60; //10 minutes
            }

        }
        catch (SalesOrderMalformed e)
        {
            e.printStackTrace();
            assertEquals("SalesOrderMalformed", "Bad data");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assertEquals("Bad data", "fail");
        }
    }

    private void processMarketTrades(TradeInstance tradeInstance)
    {
        generatedTrade = tradeInstance;
    }

    private void onError(Throwable throwable) {
        assertEquals("test failed", throwable.getMessage());
    }
}
