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

public class MarketQueueService
{
    private static final long MARKET_INTERVAL = 10*60; // 10 minutes
    private static final int TRADE_AMOUNT = 10;
    private static final String MARKET_QUEUE_URL = "https://i6pk618b7f.execute-api.ap-southeast-1.amazonaws.com/test/abc"; //abc?start=12&count=11&count=3

    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    private Disposable marketQueueProcessing;
    private Context context;

    private ApiMarketQueue marketQueueConnector;

    public MarketQueueService(Context ctx, OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository,
                              PasswordStore passwordStore) {
        this.context = ctx;
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;

        buildConnector();
    }

    private void buildConnector()
    {
//        marketQueueConnector = new Retrofit.Builder()
//                .baseUrl(MARKET_QUEUE_URL)
//                .client(httpClient)
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                .build()
//                .create(ApiMarketQueue.class);
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
        sendMarketOrders(trades)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse);

        for (TradeInstance t : trades) {
            System.out.println("SIG: " + t.getStringSig());
        }
    }

    private void handleResponse(okhttp3.Response response)
    {
        System.out.println("handle response");
        //we can push to UI from here
        BaseViewModel.onQueueUpdate(100);
        //send message
        BaseViewModel.onPushToast("Queue written");
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

    public Single<okhttp3.Response> sendMarketOrders(TradeInstance[] trades)
    {
        return Single.fromCallable(() -> {
            if (trades == null || trades.length == 0)
            {
                return null;
            }

            okhttp3.Response response = null;

            try
            {
                TradeInstance t = trades[0];

                byte[] trade = getTradeBytes(t.price, t.expiry, t.tickets, t.contractAddress);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(buffer);
                ds.write(trade);

                //now add the signatures
                for (TradeInstance thisTrade : trades)
                {
                    ds.write(thisTrade.getSignatureBytes());
                }

                ds.flush();

                Map<String, String> paramData = new HashMap<>();
                paramData.put("start", "345345");
                paramData.put("count", String.valueOf(trades.length));

                String args = formEncodedData(paramData); // = "abc?start=630832800312;count=";

                String url = MARKET_QUEUE_URL + args + String.valueOf(trades.length);

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

            OkHttpClient client = new OkHttpClient();

            RequestBody body = RequestBody.create(DATA, data);

            Request request = new Request.Builder()
                    .url(writeURL)
                    .put(body)
                    .addHeader("Content-Type", "application/vnd.awallet-signed-orders-v0")
                    .build();

            response = client.newCall(request).execute();

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
    public Single<TradeInstance> sign(Wallet wallet, String password, TradeInstance t) {
        return transactionRepository.getSignature(wallet, t.getTradeData(), password)
                .map(sig -> new TradeInstance(t, sig));
    }

    public Single<TradeInstance[]> tradesInnerLoop(Wallet wallet, String password, BigInteger price, short[] tickets, Ticket ticket) {
        return Single.fromCallable(() ->
        {
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
            byte[] trade = getTradeBytes(price, expiryTimestamp, tickets, ticket.getIntAddress());
            return new TradeInstance(price, expiryTimestamp, tickets, ticket, trade);
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

    private String formEncodedData(Map<String, String> data)
    {
        StringBuilder sb = new StringBuilder();
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
                sb.append("&");
            }

            sb.append(key + "=" + value);
        }

        return sb.toString();
    }
}
