package com.alphawallet.app.service;

import android.util.Log;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Ticket;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.ParseMagicLink;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import timber.log.Timber;

import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.token.tools.ParseMagicLink.currencyLink;
import static com.alphawallet.token.tools.ParseMagicLink.spawnable;

public class AlphaWalletService
{
    private final OkHttpClient httpClient;
    private final TransactionRepositoryType transactionRepository;
    private final Gson gson;
    private ParseMagicLink parser;

    private static final String API = "api/";
    private static final String XML_VERIFIER_ENDPOINT = "https://aw.app/api/v1/verifyXMLDSig";
    private static final String XML_VERIFIER_PASS = "pass";
    private static final MediaType MEDIA_TYPE_TOKENSCRIPT
            = MediaType.parse("text/xml; charset=UTF-8");

    public AlphaWalletService(OkHttpClient httpClient,
                              TransactionRepositoryType transactionRepository,
                              Gson gson) {
        this.httpClient = httpClient;
        this.transactionRepository = transactionRepository;
        this.gson = gson;
    }

    private void initParser()
    {
        if (parser == null)
        {
            parser = new ParseMagicLink(new CryptoFunctions(), EthereumNetworkRepository.extraChains());
        }
    }

    public Observable<Integer> handleFeemasterImport(String url, Wallet wallet, long chainId, MagicLinkData order)
    {
        switch (order.contractType)
        {
            case spawnable:
                return sendFeemasterTransaction(url, chainId, wallet.address, order.expiry,
                        "", order.signature, order.contractAddress, order.tokenIds).toObservable(); //empty string for spawn
            case currencyLink:
                return sendFeemasterCurrencyTransaction(url, chainId, wallet.address, order);
            default:
                return generateTicketString(order.indices)
                        .flatMap(ticketStr -> sendFeemasterTransaction(url, chainId, wallet.address, order.expiry,
                                ticketStr, order.signature, order.contractAddress, order.tokenIds))
                        .toObservable();
        }
    }

    /**
     * Use API to determine tokenscript validity
     * @param tokenScriptFile
     * @return
     */
    public XMLDsigDescriptor checkTokenScriptSignature(File tokenScriptFile)
    {
        XMLDsigDescriptor dsigDescriptor = new XMLDsigDescriptor();
        dsigDescriptor.result = "fail";
        try
        {
            RequestBody body = RequestBody.Companion.create(tokenScriptFile, MEDIA_TYPE_TOKENSCRIPT);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "tokenscript", body)
                    .build();

            Request request = new Request.Builder().url(XML_VERIFIER_ENDPOINT)
                    .post(requestBody)
                    .build();

            okhttp3.Response response = httpClient.newCall(request).execute();

            String result = response.body().string();
            JsonObject obj = gson.fromJson(result, JsonObject.class);
            String queryResult = obj.get("result").getAsString();
            if (queryResult.equals(XML_VERIFIER_PASS))
            {
                dsigDescriptor = gson.fromJson(result, XMLDsigDescriptor.class);
                //interpret subject to get the primary certifying body
                String[] certifiers = dsigDescriptor.subject.split(",");
                if (certifiers[0] != null && certifiers[0].length() > 3 && certifiers[0].startsWith("CN="))
                {
                    dsigDescriptor.certificateName = certifiers[0].substring(3);
                }
            }
            else
            {
                dsigDescriptor.subject = obj.get("failureReason").getAsString();
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return dsigDescriptor;
    }

    private Observable<Integer> sendFeemasterCurrencyTransaction(String url, long networkId, String address, MagicLinkData order)
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
                Timber.e(e);
            }

            return result;
        });
    }

    private Single<int[]> generateTicketArray(String indices, Ticket ticket)
    {
        return Single.fromCallable(() -> {
            List<Integer> ticketIndices = Utils.stringIntsToIntegerList(indices);
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
        if (sigData != null)
        {
            args.put("r", Numeric.toHexString(sigData.getR()));
            args.put("s", Numeric.toHexString(sigData.getS()));
            args.put("v", Numeric.toHexString(sigData.getV()));
        }
    }

    private Single<Integer> sendFeemasterTransaction(
            String url,
            long networkId,
            String toAddress,
            long expiry,
            String indices,
            byte[] tradeSig,
            String contractAddress,
            List<BigInteger> tokenIds
    ) {
        return Single.fromCallable(() -> {
            Integer result = 500; //fail by default
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(url);
                Map<String, String> args = new HashMap<>();
                if (indices.equals(""))
                {
                    sb.append("/claimSpawnableToken/");
                    args.put("tokenIds", parseTokenIds(tokenIds));
                }
                else
                {
                    sb.append("/claimToken/");
                    args.put("indices", indices);
                }
                args.put("contractAddress", contractAddress);
                args.put("address", toAddress);
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

    private String parseTokenIds(List<BigInteger> tokens)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (BigInteger index : tokens)
        {
            if (!first) sb.append(",");
            sb.append(Numeric.toHexStringNoPrefix(index));
            first = false;
        }
        return sb.toString();
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

    public Single<Boolean> checkFeemasterService(String url, long chainId, String address)
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
                    Timber.tag("RESP").d(response.body().string());
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
