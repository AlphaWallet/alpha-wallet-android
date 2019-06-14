package io.stormbird.wallet.service;

import android.util.Log;

import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.stormbird.wallet.entity.CryptoFunctions;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.stormbird.token.entity.MagicLinkData;
import io.stormbird.token.tools.ParseMagicLink;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static io.stormbird.token.tools.ParseMagicLink.currencyLink;
import static io.stormbird.token.tools.ParseMagicLink.spawnable;
import static io.stormbird.wallet.service.MarketQueueService.sigFromByteArray;

public class FeeMasterService
{
    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final PasswordStore passwordStore;
    private ParseMagicLink parser;

    private static final String API = "api/";

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
            parser = new ParseMagicLink(new CryptoFunctions());
        }
    }

    public Observable<Integer> handleFeemasterImport(String url, Wallet wallet, int chainId, MagicLinkData order)
    {
        switch (order.contractType)
        {
            case spawnable:
                return sendFeemasterTransaction(url, chainId, wallet.address, order.expiry, "", order.signature).toObservable(); //empty string for spawn
            case currencyLink:
                return sendFeemasterCurrencyTransaction(url, chainId, wallet.address, order);
            default:
                return generateTicketString(order.tickets)
                        .flatMap(ticketStr -> sendFeemasterTransaction(url, chainId, wallet.address, order.expiry, ticketStr, order.signature))
                        .toObservable();
        }
    }

    private Observable<Integer> sendFeemasterCurrencyTransaction(String url, int networkId, String address, MagicLinkData order)
    {
        return Observable.fromCallable(() -> {
            Integer result = 500; //fail by default
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(url);
                sb.append("claimFreeCurrency");
                Map<String, String> args = new HashMap<>();
                args.put("prefix", Numeric.toHexString(order.prefix));
                args.put("recipient", address);
                args.put("amount", order.amount.toString(10));
                args.put("expiry", String.valueOf(order.expiry));
                args.put("nonce", order.nonce.toString(10));
                args.put("networkId", String.valueOf(networkId));
                addSignature(args, order.signature);
                args.put("contractAddress", order.contractAddress);
                result = postRequest(sb, args);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        });
    }

    private Single<byte[]> getTradeSig(Wallet wallet, int[] indicesArray, String contractAddress, BigInteger price, long expiry, int chainId)
    {
        initParser();
        final byte[] tradeBytes = parser.getTradeBytes(indicesArray, contractAddress, price, expiry);
        return passwordStore.getPassword(wallet)
                    .flatMap(password -> transactionRepository.getSignature(wallet, tradeBytes, password, chainId));
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

    private void addSignature(Map<String, String> args, byte[] sig)
    {
        Sign.SignatureData sigData = sigFromByteArray(sig);
        args.put("r", Numeric.toHexString(sigData.getR()));
        args.put("s", Numeric.toHexString(sigData.getS()));
        args.put("v", Integer.toHexString(sigData.getV()));
    }

    private Single<Integer> sendFeemasterTransaction(String url, int networkId, String toAddress, long expiry, String indices, byte[] tradeSig) {
        return Single.fromCallable(() -> {
            Integer result = 500; //fail by default
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(url);
                if ((url.length() - url.indexOf("/api")) < 6)
                {
                    sb.append("/claimToken/");
                }
                Map<String, String> args = new HashMap<>();
                args.put("address", toAddress);
                args.put("indices", indices);
                args.put("expiry", String.valueOf(expiry));
                args.put("networkId", String.valueOf(networkId));
                addSignature(args, tradeSig);
                result = postRequest(sb, args);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        });
    }

    private Integer postRequest(StringBuilder sb, Map<String, String> args) throws Exception
    {
        MediaType mediaType = MediaType.parse("application/octet-stream");
        sb.append(formPrologData(args));

        Request request = new Request.Builder()
                .url(sb.toString())
                .post(RequestBody.create(mediaType, ""))
                .build();

        okhttp3.Response response = httpClient.newCall(request).execute();

        return response.code();
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

    public Single<Boolean> checkFeemasterService(String url, int chainId, String address)
    {
        return Single.fromCallable(() -> {
            Boolean result = false;
            try
            {
                int index = url.indexOf(API);
                if (index > 0)
                {
                    String pureServerURL = url.substring(0, index + API.length());
                    StringBuilder sb = new StringBuilder();
                    sb.append(pureServerURL);
                    sb.append("checkContractIsSupportedForFreeTransfers");
                    Map<String, String> args = new HashMap<>();
                    args.put("contractAddress", address);
                    sb.append(formPrologData(args));

                    Request request = new Request.Builder()
                            .url(sb.toString())
                            .get()
                            .build();

                    okhttp3.Response response = httpClient.newCall(request).execute();
                    int resultCode = response.code();
                    if ((resultCode/100) == 2) result = true;
                    Log.d("RESP", response.body().string());
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        });
    }
}
