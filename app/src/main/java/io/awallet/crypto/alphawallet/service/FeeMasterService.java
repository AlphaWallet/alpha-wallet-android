package io.awallet.crypto.alphawallet.service;

import android.util.Log;

import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.awallet.crypto.alphawallet.entity.CryptoFunctions;
import io.awallet.crypto.alphawallet.entity.Ticket;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.tools.ParseMagicLink;
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
    private CryptoFunctions cryptoFunctions;
    private ParseMagicLink parser;

    public FeeMasterService(OkHttpClient httpClient,
                            TransactionRepositoryType transactionRepository,
                            PasswordStore passwordStore) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.passwordStore = passwordStore;
    }

    private void initParser()
    {
        if (parser == null)
        {
            cryptoFunctions = new CryptoFunctions();
            parser = new ParseMagicLink(cryptoFunctions);
        }
    }

    //first generate and then sign the message
    public Observable<Integer> generateAndSendFeemasterTransaction(String url, Wallet wallet, String toAddress, Ticket ticket, long expiry, String indices)
    {
        return generateTicketArray(indices, ticket)
                .flatMap(indicesArray -> getTradeSig(wallet, indicesArray, ticket.getAddress(), BigInteger.ZERO, expiry))
                .flatMap(tradeSig -> sendFeemasterTransaction(url, toAddress, expiry, indices, tradeSig))
                .toObservable();
    }

    public Observable<Integer> handleFeemasterImport(String url, Wallet wallet, MagicLinkData order)
    {
        return generateTicketString(order.tickets)
                .flatMap(ticketStr -> sendFeemasterTransaction(url, wallet.address, order.expiry, ticketStr, order.signature))
                .toObservable();
    }

    private Single<byte[]> getTradeSig(Wallet wallet, int[] indicesArray, String contractAddress, BigInteger price, long expiry)
    {
        initParser();
        final byte[] tradeBytes = parser.getTradeBytes(indicesArray, contractAddress, price, expiry);
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

    private Single<String> generateTicketString(int[] tickets)
    {
        return Single.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int index : tickets)
            {
                if (!first) sb.append(",");
                sb.append(index);
                first = false;
            }
            return sb.toString();
        });
    }

    private Single<Integer> sendFeemasterTransaction(String url, String toAddress, long expiry, String indices, byte[] tradeSig) {
        return Single.fromCallable(() -> {
            Sign.SignatureData sigData = sigFromByteArray(tradeSig);
            Integer result = 500; //fail by default
            try
            {
                MediaType mediaType = MediaType.parse("application/octet-stream");
                StringBuilder sb = new StringBuilder();
                sb.append(url);
                sb.append("claimToken");
                Map<String, String> args = new HashMap<>();
                args.put("address", toAddress);
                args.put("indices", indices);
                args.put("expiry", String.valueOf(expiry));
                args.put("r", Numeric.toHexString(sigData.getR()));
                args.put("s", Numeric.toHexString(sigData.getS()));
                args.put("v", Integer.toHexString(sigData.getV()));
                sb.append(formPrologData(args));

                Request request = new Request.Builder()
                        .url(sb.toString())
                        .post(RequestBody.create(mediaType, ""))
                        .build();

                okhttp3.Response response = httpClient.newCall(request).execute();

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
