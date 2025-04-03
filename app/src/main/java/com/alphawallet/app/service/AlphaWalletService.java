package com.alphawallet.app.service;

import static com.alphawallet.app.entity.CryptoFunctions.sigFromByteArray;
import static com.alphawallet.token.tools.ParseMagicLink.currencyLink;
import static com.alphawallet.token.tools.ParseMagicLink.spawnable;
import static org.web3j.protocol.http.HttpService.JSON_MEDIA_TYPE;

import android.util.Base64;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Ticket;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.ParseMagicLink;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONObject;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class AlphaWalletService
{
    private final OkHttpClient httpClient;
    private final Gson gson;
    private ParseMagicLink parser;
    private static final String API = "api/";
    private static final String XML_VERIFIER_ENDPOINT = "https://aw.app/api/v1/verifyXMLDSig";
    private static final String TSML_VERIFIER_ENDPOINT_STAGING = "https://doobtvjcpb8dc.cloudfront.net/tokenscript/validate";
    private static final String TSML_VERIFIER_ENDPOINT = "https://api.smarttokenlabs.com/tokenscript/validate";
    private static final String XML_VERIFIER_PASS = "pass";
    private static final MediaType MEDIA_TYPE_TOKENSCRIPT
            = MediaType.parse("text/xml; charset=UTF-8");

    public AlphaWalletService(OkHttpClient httpClient,
                              Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    private static class StatusElement
    {
        String type;
        String status;
        String statusText;
        String signingKey;

        public XMLDsigDescriptor getXMLDsigDescriptor()
        {
            XMLDsigDescriptor sig = new XMLDsigDescriptor();
            sig.result = status;
            sig.issuer = signingKey;
            sig.certificateName = statusText;
            sig.keyType = type;

            return sig;
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
     * @param scriptUri
     * @param chainId
     * @param address
     * @return
     */
    public XMLDsigDescriptor checkTokenScriptSignature(String scriptUri, long chainId, String address)
    {
        XMLDsigDescriptor dsigDescriptor = new XMLDsigDescriptor();
        dsigDescriptor.result = "fail";
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sourceType", "scriptUri");
            jsonObject.put("sourceId", chainId + "-" + Keys.toChecksumAddress(address));
            jsonObject.put("sourceUrl", scriptUri);
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON_MEDIA_TYPE);

            okhttp3.Response response = getTSValidationCheck(body);

            if ((response.code() / 100) == 2)
            {
                String result = response.body().string();
                JsonObject obj = gson.fromJson(result, JsonObject.class);
                if (obj.has("error"))
                    return dsigDescriptor;

                JsonObject overview = obj.getAsJsonObject("overview");
                if (overview != null)
                {
                    JsonArray statuses = overview.getAsJsonArray("originStatuses");
                    if (statuses.isEmpty())
                    {
                        return dsigDescriptor;
                    }

                    StatusElement status1 = gson.fromJson(statuses.get(0), StatusElement.class);
                    return status1.getXMLDsigDescriptor();
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return dsigDescriptor;
    }

    public XMLDsigDescriptor checkTokenScriptSignature(InputStream inputStream, long chainId, String address, String sourceUrl)
    {
        XMLDsigDescriptor dsigDescriptor = new XMLDsigDescriptor();
        dsigDescriptor.result = "fail";
        try
        {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sourceType", "scriptUri");
            jsonObject.put("sourceId", chainId + "-" + Keys.toChecksumAddress(address));
            jsonObject.put("sourceUrl", sourceUrl);
            jsonObject.put("base64Xml", streamToBase64(inputStream));
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON_MEDIA_TYPE);

            okhttp3.Response response = getTSValidationCheck(body);

            if ((response.code() / 100) == 2)
            {
                String result = response.body().string();
                JsonObject obj = gson.fromJson(result, JsonObject.class);
                if (obj.has("error"))
                    return dsigDescriptor;

                JsonObject overview = obj.getAsJsonObject("overview");
                if (overview != null)
                {
                    JsonArray statuses = overview.getAsJsonArray("originStatuses");
                    if (statuses.isEmpty())
                    {
                        return dsigDescriptor;
                    }

                    StatusElement status1 = gson.fromJson(statuses.get(0), StatusElement.class);
                    return status1.getXMLDsigDescriptor();
                }
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
        }

        return dsigDescriptor;
    }

    private String streamToBase64(InputStream inputStream) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, StandardCharsets.UTF_8)))
        {
            int c;
            while ((c = reader.read()) != -1)
            {
                sb.append((char) c);
            }
        }

        byte[] base64Encoded = Base64.encode(sb.toString().getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        return new String(base64Encoded);
    }

    private Response getTSValidationCheck(RequestBody body) throws Exception
    {
        Request request = new Request.Builder().url(TSML_VERIFIER_ENDPOINT)
                .post(body)
                .build();

        okhttp3.Response response = httpClient.newCall(request).execute();

        if ((response.code() / 100) != 2)
        {
            //try staging endpoint
            request = new Request.Builder().url(TSML_VERIFIER_ENDPOINT_STAGING)
                    .post(body)
                    .build();

            response = httpClient.newCall(request).execute();
        }

        return response;
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
                Timber.e(e);
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
                Timber.e(e);
            }

            return result;
        });
    }
}
