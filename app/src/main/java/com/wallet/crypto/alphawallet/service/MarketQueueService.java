package com.wallet.crypto.alphawallet.service;

import android.content.Context;
import android.os.Looper;

import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.Ticker;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TokenTicker;
import com.wallet.crypto.alphawallet.entity.TradeInstance;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import static com.wallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 7/02/2018.
 */

public class MarketQueueService {
    private static final long MARKET_INTERVAL = 10*60; // 10 minutes
    private static final int TRADE_AMOUNT = 2016;
    private static final String MARKET_QUEUE_URL = "https://i6pk618b7f.execute-api.ap-southeast-1.amazonaws.com/test/abc";

    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    private Disposable marketQueueProcessing;
    private ApiMarketQueue marketQueueConnector;

    public MarketQueueService(Context ctx, OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository,
                              PasswordStore passwordStore) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;

        buildConnector();
    }

    //TODO: hook up retrofit2 instead of doing
    private void buildConnector()
    {
//        marketQueueConnector = new Retrofit.Builder()
//                .baseUrl(MARKET_QUEUE_URL)
//                .client(httpClient)
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                .build()
//                .create(ApiMarketQueue.class);
    }

    private void processMarketTrades(TradeInstance trades)
    {
        marketQueueProcessing = sendMarketOrders(trades)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse);
    }

    //This is running on the main UI thread, so it's safe to push messages etc here
    private void handleResponse(okhttp3.Response response)
    {
        System.out.println("handle response");
        BaseViewModel.onQueueUpdate(100);
        if (response.code() == HttpURLConnection.HTTP_OK)
        {
            BaseViewModel.onPushToast("Queue written");
        }
        else
        {
            BaseViewModel.onPushToast("ERROR: Trade not processed");
        }

        marketQueueProcessing.dispose();
    }

    private byte[] getTradeBytes(BigInteger price, BigInteger expiryTimestamp, short[] tickets, BigInteger contractAddr) throws Exception
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(buffer);
        ds.write(Numeric.toBytesPadded(contractAddr, 32));
        ds.write(Numeric.toBytesPadded(price, 32));
        ds.write(Numeric.toBytesPadded(expiryTimestamp, 32));
        for (short ticketIndex : tickets)
        {
            ds.writeShort(ticketIndex);
        }
        ds.flush();

        return buffer.toByteArray();
    }

    private Single<okhttp3.Response> sendMarketOrders(TradeInstance trades)
    {
        return Single.fromCallable(() -> {
            if (trades == null || trades.getSignatures().size() == 0)
            {
                return null;
            }

            okhttp3.Response response = null;

            try
            {
                byte[] trade = getTradeBytes(trades.price, trades.expiry, trades.tickets, trades.contractAddress);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(buffer);
                ds.write(trade);
                trades.addSignatures(ds);
                ds.flush();

                Map<String, String> paramData = new HashMap<>();
                paramData.put("start", "345345");
                paramData.put("count", String.valueOf(trades.sigCount()));
                String args = formEncodedData(paramData);
                String url = MARKET_QUEUE_URL + args;
                response = writeToQueue(url, buffer.toByteArray(), true);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return response;
        });
    }

    //TODO: Refactor this using
    private okhttp3.Response writeToQueue(final String writeURL, final byte[] data, final boolean post)
    {
        okhttp3.Response response = null;
        try
        {
            final MediaType DATA
                    = MediaType.parse("application/vnd.awallet-signed-orders-v0");

            RequestBody body = RequestBody.create(DATA, data);
            Request request = new Request.Builder()
                    .url(writeURL)
                    .put(body)
                    .addHeader("Content-Type", "application/vnd.awallet-signed-orders-v0")
                    .build();

            response = httpClient.newCall(request).execute();

            System.out.println("HI: " + response.message());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return response;
    }

    public interface ApiMarketQueue
    {
        @POST("abc?start=12&count=11&count=3")
        Single<Response<String>> sendMarketMessage(@Body byte[] body);
    }

    //sign a trade transaction
    public Single<byte[]> sign(Wallet wallet, String password, TradeInstance t, byte[] data) {
        return transactionRepository.getSignature(wallet, data, password);
    }

    private Single<TradeInstance> tradesInnerLoop(Wallet wallet, String password, BigInteger price, short[] tickets, Ticket ticket) {
        return Single.fromCallable(() ->
        {
            long initialExpiry = System.currentTimeMillis() / 1000L + MARKET_INTERVAL;
            TradeInstance trade = new TradeInstance(price, BigInteger.valueOf(initialExpiry), tickets, ticket.getAddress());

            //initial expiry 10 minutes from now

            //TODO: replace this with a computation observable something like this:
//            Flowable.range(0, TRADE_AMOUNT)
//                    .observeOn(Schedulers.computation())
//                    .map(v -> getTradeMessageAndSignature...)
//                    .blockingSubscribe(this::addTradeSequence, this::onError, this::onAllTransactions);

            transactionRepository.unlockAccount(wallet, password);
            for (int i = 0; i < TRADE_AMOUNT; i++)
            {
                BigInteger expiryTimestamp = BigInteger.valueOf(initialExpiry + (i * MARKET_INTERVAL));
                trade.addSignature(getTradeSignature(wallet, password, price, expiryTimestamp, tickets, ticket).blockingGet());
                float upd = ((float)i/TRADE_AMOUNT)*100.0f;
                BaseViewModel.onQueueUpdate((int)upd);
            }
            transactionRepository.lockAccount(wallet, password);
            return trade;
        });
    }

    private Single<TradeInstance> getTradeMessages(Wallet wallet, BigInteger price, short[] tickets, Ticket ticket) {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> tradesInnerLoop(wallet, password, price, tickets, ticket));
    }

    private Single<byte[]> getTradeSignature(Wallet wallet, String password, BigInteger price, BigInteger expiryTimestamp, short[] tickets, Ticket ticket) {
        return encodeMessageForTrade(price, expiryTimestamp, tickets, ticket)
                .flatMap(tradeBytes -> transactionRepository.getSignatureFast(wallet, tradeBytes, password));
    }

    private Single<byte[]> encodeMessageForTrade(BigInteger price, BigInteger expiryTimestamp, short[] tickets, Ticket ticket) {
        return Single.fromCallable(() -> getTradeBytes(price, expiryTimestamp, tickets, ticket.getIntAddress()));
    }

    public Observable<TradeInstance> getTradeInstances(Wallet wallet, BigInteger price, short[] tickets, Ticket ticket) {
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

    public void createMarketOrders(Wallet wallet, BigInteger price, short[] ticketIDs, Ticket ticket)
    {
        marketQueueProcessing = getTradeInstances(wallet, price, ticketIDs, ticket)
                .subscribe(this::processMarketTrades, this::onError, this::onAllTransactions);
    }

    private void onError(Throwable error) {
        //something went wrong
    }

    private void onAllTransactions()
    {

    }

    private String formEncodedData(Map<String, String> data)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (String key : data.keySet())
        {
            String value = null;
            try
            {
                value = URLEncoder.encode(data.get(key), "UTF-8");
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }

            if (sb.length() > 0)
            {
                sb.append(";");
            }

            sb.append(key + "=" + value);
        }

        return sb.toString();
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
