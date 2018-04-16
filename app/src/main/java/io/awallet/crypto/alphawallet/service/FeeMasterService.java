package io.awallet.crypto.alphawallet.service;

import android.content.Context;
import android.util.Log;

import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.awallet.crypto.alphawallet.entity.SalesOrder;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static io.awallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

public class FeeMasterService
{
    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;

    public FeeMasterService(OkHttpClient httpClient,
                            TransactionRepositoryType transactionRepository,
                            PasswordStore passwordStore) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    //first generate and then sign the message
    public Observable<Integer> generateAndSendFeemasterTransaction(Wallet wallet, String toAddress, Ticket ticket, long expiry, String indices)
    {
        return generateTicketArray(indices, ticket)
                .flatMap(indicesArray -> getTradeSig(wallet, indicesArray, ticket.getAddress(), BigInteger.ZERO, expiry))
                .flatMap(tradeSig -> sendFeemasterTransaction(toAddress, expiry, indices, tradeSig))
                .toObservable();
    }

    private Single<byte[]> getTradeSig(Wallet wallet, int[] indicesArray, String contractAddress, BigInteger price, long expiry)
    {
        final byte[] tradeBytes = SalesOrder.getTradeBytes(indicesArray, contractAddress, price, expiry);
        return passwordStore.getPassword(wallet)
                    .flatMap(password -> transactionRepository.getSignature(wallet, tradeBytes, password));
    }

    private Single<int[]> generateTicketArray(String indices, Ticket ticket)
    {
        return Single.fromCallable(() -> {
            List<Integer> ticketIndices = ticket.stringIntsToIntegerList(indices);
            int[] indicesArray = new int[ticketIndices.size()];
            for (int i = 0; i < ticketIndices.size(); i++) indicesArray[i] = ticketIndices.get(i);
            return indicesArray;
        });
    }

    private Single<Integer> sendFeemasterTransaction(String toAddress, long expiry, String indices, byte[] tradeSig) {
        return Single.fromCallable(() -> {
            Sign.SignatureData sigData = sigFromByteArray(tradeSig);
            okhttp3.Response response = null;
            Integer result = 500;
            MediaType mediaType = MediaType.parse("application/octet-stream");

            String v = Integer.toHexString(sigData.getV());

            StringBuilder sb = new StringBuilder();
            sb.append("http://feemaster.eastasia.cloudapp.azure.com:8080/api/claimToken");
            //sb.append("http://stormbird.duckdns.org:8080/api/claimToken");
            Map<String, String> args = new HashMap<>();
            args.put("address", toAddress);
            args.put("indices", indices);
            args.put("expiry", String.valueOf(expiry));
            args.put("r", Numeric.toHexString(sigData.getR()));
            args.put("s", Numeric.toHexString(sigData.getS()));
            args.put("v", v);
            sb.append(formPrologData(args));

            try
            {
                Request request = new Request.Builder()
                        .url(sb.toString())
                        .post(RequestBody.create(mediaType, ""))
                        .build();

                response = httpClient.newCall(request).execute();

                result = response.code();
                Log.d("RESP", response.body().string());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        });
    }

    private String formPrologData(Map<String, String> data)
    {
        StringBuilder sb = new StringBuilder();

        for (String key : data.keySet())
        {
            String value = data.get(key);//URLEncoder.encode(data.get(key), "UTF-8");
            if (sb.length() > 0)
            {
                sb.append("&");
            }
            else
            {
                sb.append("?");
            }

            sb.append(key);
            sb.append("=");
            sb.append(value);
        }

        return sb.toString();
    }
}
