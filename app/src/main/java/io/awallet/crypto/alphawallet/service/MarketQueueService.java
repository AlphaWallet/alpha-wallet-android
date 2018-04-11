package io.awallet.crypto.alphawallet.service;

import android.content.Context;
import android.os.Looper;

import com.fasterxml.jackson.databind.util.ArrayBuilders;
import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.BaseViewCallback;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.EthereumReadBuffer;
import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.Ticker;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.TokenTicker;
import io.awallet.crypto.alphawallet.entity.TradeInstance;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;

import org.json.JSONArray;
import org.json.JSONObject;
import org.spongycastle.jce.interfaces.ECKey;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.SYSTEM_HEALTH_SERVICE;
import static io.awallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 7/02/2018.
 */

public class MarketQueueService {
    private static final long MARKET_INTERVAL = 10*60; // 10 minutes
    private static final int TRADE_AMOUNT = 1008;
    private static final String MARKET_QUEUE_URL = "https://482kdh4npg.execute-api.ap-southeast-1.amazonaws.com/dev/";
    private static final String MARKET_QUEUE_FETCH = MARKET_QUEUE_URL + "contract/";

    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    private Disposable marketQueueProcessing;
    private ApiMarketQueue marketQueueConnector;
    private BaseViewCallback messageCallback;

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
        marketQueueProcessing = sendSalesOrders(trades)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse);
    }

    //This is running on the main UI thread, so it's safe to push messages etc here
    private void handleResponse(String response)
    {
        messageCallback.queueUpdate(100);
        //TODO: Handle response correctly
        //"{\"orders\": {\"received\": 200, \"accepted\": 200}, \"1st_order\": \"00000000000000000000000000000000000000000000000003ff2e795f500000000000000000000000000000000000000000000000000000000000005ab1b21c0b6732baecc0793e38a98934799abd3c7dc3cf3100d300d4\"}"
        if (response.contains("accepted"))//  == HttpURLConnection.HTTP_OK)
        {
            messageCallback.showMarketQueueSuccessDialog(R.string.dialog_marketplace_success);
        }
        else
        {
            messageCallback.showMarketQueueErrorDialog(R.string.dialog_process_error);
        }

        marketQueueProcessing.dispose();
    }

    private Single<String> sendSalesOrders(TradeInstance trades)
    {
        return Single.fromCallable(() -> {
            if (trades == null || trades.getSignatures().size() == 0)
            {
                return null;
            }

            String response = null;

            try
            {
                byte[] trade = trades.getTradeBytes();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream ds = new DataOutputStream(buffer);
                ds.write(trade);
                trades.addSignatures(ds);
                ds.flush();

                Map<String, String> prologData = new HashMap<>();
                prologData.put("public-key", trades.publicKey)  ;
                String urlProlog = formPrologData(prologData)   ;

                Map<String, String> paramData = new HashMap<>();
                paramData.put("start", String.valueOf(trades.ticketStart)); //start ticket ID
                paramData.put("count", String.valueOf(trades.tickets.length));
                String args = formEncodedData(paramData);
                String url = MARKET_QUEUE_URL + urlProlog + args;
                response = writeToQueue(url, buffer.toByteArray(), true);

//                String ownerAddress = "";
//                //see if we can extract the user address
//                Sign.SignatureData sigData = sigFromByteArray(trades.getSignatureBytes(0));
//                BigInteger recoveredKey = Sign.signedMessageToKey(trade, sigData);
//                ownerAddress = "0x" + Keys.getAddress(recoveredKey);
//                System.out.println(ownerAddress);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return response;
        });
    }

    //TODO: Refactor this using
    private String writeToQueue(final String writeURL, final byte[] data, final boolean post)
    {
        String result = null;
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

            okhttp3.Response response = httpClient.newCall(request).execute();
            result = response.body().string();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private String readFromQueue(final String contractAddr)
    {
        okhttp3.Response response = null;
        String result = null;
        try
        {
            String fullUrl = MARKET_QUEUE_FETCH + contractAddr;

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build();

            response = httpClient.newCall(request).execute();

            result = response.body().string();

            //System.out.println("HI: " + result);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public byte[] generateReverseTradeData(Wallet wallet, SalesOrder marketInstance)
    {
        byte[] data = null;
        try
        {
            BigInteger expiry = BigInteger.valueOf(marketInstance.expiry);
            List<BigInteger> ticketIndices = new ArrayList<>();
            for (int ticketIndex : marketInstance.tickets) {
                ticketIndices.add(BigInteger.valueOf(ticketIndex));
            }
            //convert to signature representation
            Sign.SignatureData sellerSig = sigFromByteArray(marketInstance.signature);

            data = TokenRepository.createTrade(expiry, ticketIndices, (int)sellerSig.getV(), sellerSig.getR(), sellerSig.getS());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return data;
    }

    public void setCallback(BaseViewCallback callback) {
        messageCallback = callback;
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

    private Single<TradeInstance> tradesInnerLoop(Wallet wallet, String password, BigInteger price, int[] tickets, String contractAddr, BigInteger firstTicketId) {
        return Single.fromCallable(() ->
        {
            long initialExpiry = (System.currentTimeMillis() / 1000L) + MARKET_INTERVAL;
            //TODO: replace this with a computation observable something like this:
//            Flowable.range(0, TRADE_AMOUNT)
//                    .observeOn(Schedulers.computation())
//                    .map(v -> getTradeMessageAndSignature...)
//                    .blockingSubscribe(this::addTradeSequence, this::onError, this::onAllTransactions);

            transactionRepository.unlockAccount(wallet, password);
            //Recover public key
            BigInteger recoveredKey = ecRecoverPublicKey(wallet, password);

            TradeInstance trade = new TradeInstance(price, BigInteger.valueOf(initialExpiry), tickets, contractAddr, recoveredKey, firstTicketId);

            for (int i = 0; i < TRADE_AMOUNT; i++)
            {
                trade.expiry =  BigInteger.valueOf(initialExpiry + (i * MARKET_INTERVAL));
                trade.addSignature(getTradeSignature(wallet, password, trade).blockingGet());
                float upd = ((float)i/TRADE_AMOUNT)*100.0f;
                messageCallback.queueUpdate((int)upd);
            }
            transactionRepository.lockAccount(wallet, password);
            trade.expiry = BigInteger.valueOf(initialExpiry); //ensure expiry of first order is correct
            return trade;
        });
    }

    private Single<TradeInstance> getTradeMessages(Wallet wallet, BigInteger price, int[] tickets, String contractAddr, BigInteger firstTicketId) {
        return passwordStore.getPassword(wallet)
                .flatMap(password -> tradesInnerLoop(wallet, password, price, tickets, contractAddr, firstTicketId));
    }

    private Single<byte[]> getTradeSignature(Wallet wallet, String password, TradeInstance trade) {
        return encodeMessageForTrade(trade)
                .flatMap(tradeBytes -> transactionRepository.getSignatureFast(wallet, tradeBytes, password));
    }

    private Single<byte[]> encodeMessageForTrade(TradeInstance trade) {
        return Single.fromCallable(trade::getTradeBytes);
    }

    public Observable<TradeInstance> getTradeInstances(Wallet wallet, BigInteger price, int[] tickets, String contractAddr, BigInteger firstTicketId) {
        return getTradeMessages(wallet, price, tickets, contractAddr, firstTicketId).toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data) {
        return passwordStore.getPassword(from)
                .flatMap(password ->
                        transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, password)
                                .observeOn(AndroidSchedulers.mainThread()));
    }

    public void createSalesOrders(Wallet wallet, BigInteger price, int[] ticketIDs, String contractAddr, BigInteger firstTicketId, BaseViewCallback callback) {
        messageCallback = callback;
        marketQueueProcessing = getTradeInstances(wallet, price, ticketIDs, contractAddr, firstTicketId)
                .subscribe(this::processMarketTrades, this::onError, this::onAllTransactions);
    }

    public Observable<SalesOrder[]> fetchSalesOrders(String contractAddress) {
        return Single.fromCallable(() -> {
            String result = readFromQueue(contractAddress);

//            {
//                "orders": [
//                {
//                    "price": "0.000000000000000003",
//                        "start": "3",
//                        "stop": "5",
//                        "count": "3",
//                        "expiry": "1519173576",
//                        "message": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWoy/yLyaECakvG8LqLvkhtHQnaVzKznkABcAGAAZ)",
//                        "signature": "m/LOtWSLBuSNUSM/3jsrEiwddob1AEuUQsXRI/urgmAIxgoXIHa5n8Vu1jfqdc/Smz6M0QDUCHM+R3p0L6ygKgA="
//                }
//    ]
//            }

            JSONObject stateData = new JSONObject(result);
            JSONArray orders = stateData.getJSONArray("orders");

            SalesOrder[] trades = new SalesOrder[orders.length()];

            for (int i = 0; i < orders.length(); i++)
            {
                JSONObject order = (JSONObject)orders.get(i);
                double price = order.getDouble("price");
                int start = order.getInt("start");
                int stop = order.getInt("stop");
                int count = order.getInt("count");
                long expiry = order.getLong("expiry");
                String base64Msg = order.getString("message");
                String base64Sig = order.getString("signature");

                trades[i] = new SalesOrder(price, expiry, start, count, contractAddress, base64Sig, base64Msg);
            }

            return trades;
        }).toObservable();
    }

    private void onError(Throwable error) {
        //something went wrong
    }

    private void onAllTransactions()
    {

    }

    private String formPrologData(Map<String, String> data)
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
                sb.append("/");
            }

            sb.append(key + "/" + value);
        }

        return sb.toString();
    }

    private String formEncodedData(Map<String, String> data)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        boolean first = true;
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

            if (!first)
            {
                sb.append(";");
            }
            else
            {
                first = false;
            }

            sb.append(key + "=" + value);
        }

        return sb.toString();
    }

    private BigInteger ecRecoverPublicKey(Wallet wallet, String password) throws Exception
    {
        String testSigMsg = "obtain public key";
        byte[] testSigBytes = transactionRepository.getSignatureFast(wallet, testSigMsg.getBytes(), password).blockingGet();
        Sign.SignatureData testSig = sigFromByteArray(testSigBytes);
        BigInteger recoveredKey = Sign.signedMessageToKey(testSigMsg.getBytes(), testSig);
        String publicKeyString = Keys.getAddress(recoveredKey); //TODO: Remove - this is here for debug/testing

        return recoveredKey;
    }

    public static Sign.SignatureData sigFromByteArray(byte[] sig)
    {
        byte   subv = (byte)(sig[64]);
        if (subv < 27) subv += 27;

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);

        BigInteger r = new BigInteger(1, subrRev);
        BigInteger s = new BigInteger(1, subsRev);

        Sign.SignatureData ecSig = new Sign.SignatureData(subv, subrRev, subsRev);

        return ecSig;
    }
}
