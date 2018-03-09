package com.wallet.crypto.alphawallet.service;


import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;
import com.wallet.crypto.alphawallet.viewmodel.BaseViewModel;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Arrays;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenService {
    private static final long MARKET_INTERVAL = 10*60; // 10 minutes
    private static final int TRADE_AMOUNT = 2016;
    private static final String IMPORT_TOKEN_URL = "http://stormbird.duckdns.org:8080/api";
    private static final String IMPORT_ARG = "/importTicket";

    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    private Disposable marketQueueProcessing;
    private MarketQueueService.ApiMarketQueue marketQueueConnector;

    public ImportTokenService(OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository,
                              PasswordStore passwordStore) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    private void handleTicketImport(Wallet wallet, String importMessage)
    {
        //1. generate the signature (observe)
        //2. package up the params
        //3. send the import link to server (observe)

//        marketQueueProcessing = sendImportData(trades)
//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(this::handleResponse);
    }

    //This is running on the main UI thread, so it's safe to push messages etc here
    private void handleResponse(String response)
    {
        System.out.println("handle response");
        BaseViewModel.onQueueUpdate(100);
        if (response.contains("success"))//  == HttpURLConnection.HTTP_OK)
        {
            BaseViewModel.onPushToast("Queue written");
        }
        else
        {
            BaseViewModel.onPushToast("ERROR: Trade not processed");
        }

        marketQueueProcessing.dispose();
    }

//    private Single<String> sendImportData(TradeInstance trades)
//    {
//        return Single.fromCallable(() -> {
//            if (trades == null || trades.getSignatures().size() == 0)
//            {
//                return null;
//            }
//
//            String response = null;
//
//            try
//            {
//                byte[] trade = getTradeBytes(trades.price, trades.expiry, trades.tickets, trades.contractAddress);
//
//                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//                DataOutputStream ds = new DataOutputStream(buffer);
//                ds.write(trade);
//                trades.addSignatures(ds);
//                ds.flush();
//
//                Map<String, String> prologData = new HashMap<>();
//                prologData.put("public-key", trades.publicKey)  ;
//                String urlProlog = formPrologData(prologData)   ;
//
//                Map<String, String> paramData = new HashMap<>();
//                paramData.put("start", String.valueOf(trades.ticketStart)); //start ticket ID
//                paramData.put("count", String.valueOf(trades.tickets.length));
//                String args = formEncodedData(paramData);
//                String url = MARKET_QUEUE_URL + urlProlog + args;
//                response = writeToQueue(url, buffer.toByteArray(), true);
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//
//            return response;
//        });
//    }

//    //TODO: Refactor this using
//    private String writeToQueue(final String writeURL, final byte[] data, final boolean post)
//    {
//        String result = null;
//        try
//        {
//            final MediaType DATA
//                    = MediaType.parse("application/vnd.awallet-signed-orders-v0");
//
//            RequestBody body = RequestBody.create(DATA, data);
//            Request request = new Request.Builder()
//                    .url(writeURL)
//                    .put(body)
//                    .addHeader("Content-Type", "application/vnd.awallet-signed-orders-v0")
//                    .build();
//
//            okhttp3.Response response = httpClient.newCall(request).execute();
//            result = response.body().string();
//            System.out.println("HI: " + result);
//        }
//        catch(Exception e)
//        {
//            e.printStackTrace();
//        }
//
//        return result;
//    }

    //sign the ticket data
    public Single<byte[]> sign(Wallet wallet, String password, byte[] data) {
        return transactionRepository.getSignature(wallet, data, password);
    }

//
//    public Single<SalesOrder[]> fetchSalesOrders(String contractAddress) {
//        return Single.fromCallable(() -> {
//            String result = readFromQueue(contractAddress);
//
////            {
////                "orders": [
////                {
////                    "price": "0.000000000000000003",
////                        "start": "3",
////                        "stop": "5",
////                        "count": "3",
////                        "expiry": "1519173576",
////                        "message": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAWoy/yLyaECakvG8LqLvkhtHQnaVzKznkABcAGAAZ)",
////                        "signature": "m/LOtWSLBuSNUSM/3jsrEiwddob1AEuUQsXRI/urgmAIxgoXIHa5n8Vu1jfqdc/Smz6M0QDUCHM+R3p0L6ygKgA="
////                }
////    ]
////            }
//
//            JSONObject stateData = new JSONObject(result);
//            JSONArray orders = stateData.getJSONArray("orders");
//
//            SalesOrder[] trades = new SalesOrder[orders.length()];
//
//            for (int i = 0; i < orders.length(); i++)
//            {
//                JSONObject order = (JSONObject)orders.get(i);
//                double price = order.getDouble("price");
//                int start = order.getInt("start");
//                int stop = order.getInt("stop");
//                int count = order.getInt("count");
//                long expiry = order.getLong("expiry");
//                String base64Msg = order.getString("message");
//                String base64Sig = order.getString("signature");
//
//                trades[i] = new SalesOrder(price, expiry, start, count, contractAddress, base64Sig, base64Msg);
//            }
//
//            return trades;
//        });
//    }

    private BigInteger ecRecoverPublicKey(Wallet wallet, String password) throws Exception
    {
        String testSigMsg = "obtain public key";
        byte[] testSigBytes = transactionRepository.getSignatureFast(wallet, testSigMsg.getBytes(), password).blockingGet();
        Sign.SignatureData testSig = sigFromByteArray(testSigBytes);
        BigInteger recoveredKey = Sign.signedMessageToKey(testSigMsg.getBytes(), testSig);
        String publicKeyString = Keys.getAddress(recoveredKey); //TODO: Remove - this is here for debug/testing

        return recoveredKey;
    }

    public static Sign.SignatureData sigFromByteArray(byte[] sig) throws Exception
    {
        byte   subv = (byte)(sig[64] + 27);

        byte[] subrRev = Arrays.copyOfRange(sig, 0, 32);
        byte[] subsRev = Arrays.copyOfRange(sig, 32, 64);

        BigInteger r = new BigInteger(1, subrRev);
        BigInteger s = new BigInteger(1, subsRev);

        Sign.SignatureData ecSig = new Sign.SignatureData(subv, subrRev, subsRev);

        return ecSig;
    }
}
