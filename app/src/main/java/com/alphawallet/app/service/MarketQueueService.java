package com.alphawallet.app.service;

import android.content.Context;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.BaseViewCallback;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.TradeInstance;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.MessageData;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.ParseMagicLink;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
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
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;

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

    private Disposable marketQueueProcessing;
    private ApiMarketQueue marketQueueConnector;
    private BaseViewCallback messageCallback;
    private ParseMagicLink parser;
    private CryptoFunctions cryptoFunctions;

    public MarketQueueService(Context ctx, OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;

        buildConnector();
    }

    private void initParser()
    {
        if (parser == null)
        {
            cryptoFunctions = new CryptoFunctions();
            //TODO get network properly if need be
            parser = new ParseMagicLink(cryptoFunctions, EthereumNetworkRepository.extraChains());
        }
    }

    //TODO: hook up retrofit2 instead of doing
    private void buildConnector()
    {

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
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return result;
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
    public Single<SignatureFromKey> sign(Wallet wallet, TradeInstance t, Signable data, int chainId) {
        return transactionRepository.getSignature(wallet, data, chainId);
    }

    private Single<TradeInstance> tradesInnerLoop(Wallet wallet, String password, BigInteger price, int[] tickets, String contractAddr, BigInteger firstTicketId, int chainId) {
        return Single.fromCallable(() ->
        {
            long initialExpiry = (System.currentTimeMillis() / 1000L) + MARKET_INTERVAL;
            //Recover public key
            BigInteger recoveredKey = ecRecoverPublicKey(wallet, password, chainId);

            TradeInstance trade = new TradeInstance(price, BigInteger.valueOf(initialExpiry), tickets, contractAddr, recoveredKey, firstTicketId);

            for (int i = 0; i < TRADE_AMOUNT; i++)
            {
                trade.expiry =  BigInteger.valueOf(initialExpiry + (i * MARKET_INTERVAL));
                trade.addSignature(getTradeSignature(wallet, password, trade, chainId).blockingGet());
                float upd = ((float)i/TRADE_AMOUNT)*100.0f;
                messageCallback.queueUpdate((int)upd);
            }
            trade.expiry = BigInteger.valueOf(initialExpiry); //ensure expiry of first order is correct
            return trade;
        });
    }

    private Single<TradeInstance> getTradeMessages(Wallet wallet, BigInteger price, int[] tickets, String contractAddr, BigInteger firstTicketId, int chainId) {
        return tradesInnerLoop(wallet, "password", price, tickets, contractAddr, firstTicketId, chainId);
    }

    private Single<byte[]> getTradeSignature(Wallet wallet, String password, TradeInstance trade, int chainId) {
        return encodeMessageForTrade(trade)
                .flatMap(tradeBytes -> transactionRepository.getSignatureFast(wallet, password, tradeBytes, chainId));
    }

    private Single<byte[]> encodeMessageForTrade(TradeInstance trade) {
        return Single.fromCallable(trade::getTradeBytes);
    }

    public Observable<TradeInstance> getTradeInstances(Wallet wallet, BigInteger price, int[] tickets, String contractAddr, BigInteger firstTicketId, int chainId) {
        return getTradeMessages(wallet, price, tickets, contractAddr, firstTicketId, chainId).toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> create(Wallet from, String to, BigInteger subunitAmount, BigInteger gasPrice, BigInteger gasLimit, byte[] data, int chainId) {
        return transactionRepository.createTransaction(from, to, subunitAmount, gasPrice, gasLimit, data, chainId)
                                .observeOn(AndroidSchedulers.mainThread());
    }

    public void createSalesOrders(Wallet wallet, BigInteger price, int[] ticketIDs, String contractAddr, BigInteger firstTicketId, BaseViewCallback callback, int chainId) {
        messageCallback = callback;
        marketQueueProcessing = getTradeInstances(wallet, price, ticketIDs, contractAddr, firstTicketId, chainId)
                .subscribe(this::processMarketTrades, this::onError, this::onAllTransactions);
    }

    public Observable<MagicLinkData[]> fetchSalesOrders(String contractAddress) {
        return Single.fromCallable(() -> {
            String result = readFromQueue(contractAddress);

            initParser();

            if (result == null) return new MagicLinkData[0];

            JSONObject stateData = new JSONObject(result);
            JSONArray orders = stateData.getJSONArray("orders");
            MagicLinkData[] trades = new MagicLinkData[orders.length()];

            for (int i = 0; i < orders.length(); i++)
            {
                MagicLinkData data = new MagicLinkData();
                JSONObject order = (JSONObject)orders.get(i);
                data.price = order.getDouble("price");
                data.ticketStart = order.getInt("start");
                int stop = order.getInt("stop");
                data.ticketCount = order.getInt("count");
                data.expiry = order.getLong("expiry");
                String base64Msg = order.getString("message");
                String base64Sig = order.getString("signature");

                data.message = cryptoFunctions.Base64Decode(base64Msg);
                MessageData msgData = parser.readByteMessage(data.message, cryptoFunctions.Base64Decode(base64Sig), data.ticketCount);
                data.priceWei = msgData.priceWei;
                data.indices = msgData.tickets;
                System.arraycopy(msgData.signature, 0, data.signature, 0, 65);
                trades[i] = data;
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

            sb.append(key).append("/").append(value);
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

            sb.append(key).append("=").append(value);
        }

        return sb.toString();
    }

    private BigInteger ecRecoverPublicKey(Wallet wallet, String password, int chainId) throws Exception
    {
        String testSigMsg = "obtain public key";
        byte[] testSigBytes = transactionRepository.getSignatureFast(wallet, password, testSigMsg.getBytes(), chainId).blockingGet();
        Sign.SignatureData testSig = sigFromByteArray(testSigBytes);
        BigInteger recoveredKey = Sign.signedMessageToKey(testSigMsg.getBytes(), testSig);
        String publicKeyString = Keys.getAddress(recoveredKey); //TODO: Remove - this is here for debug/testing

        return recoveredKey;
    }
}
