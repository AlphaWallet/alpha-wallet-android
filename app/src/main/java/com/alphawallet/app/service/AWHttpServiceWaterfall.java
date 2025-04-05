package com.alphawallet.app.service;

import static com.alphawallet.app.service.JsonValidator.validateAndGetStream;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_BAOBAB_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KLAYTN_ID;
import static okhttp3.ConnectionSpec.CLEARTEXT;

import android.text.TextUtils;

import com.google.gson.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.http.HttpService;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import timber.log.Timber;

public class AWHttpServiceWaterfall extends HttpService
{

    /**
     * Copied from {@link ConnectionSpec#APPROVED_CIPHER_SUITES}.
     */
    @SuppressWarnings("JavadocReference")
    private static final CipherSuite[] INFURA_CIPHER_SUITES =
            new CipherSuite[]{
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,

                    // Note that the following cipher suites are all on HTTP/2's bad cipher suites list.
                    // We'll
                    // continue to include them until better suites are commonly available. For example,
                    // none
                    // of the better cipher suites listed above shipped with Android 4.4 or Java 7.
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA,

                    // Additional INFURA CipherSuites
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA256
            };

    private static final ConnectionSpec INFURA_CIPHER_SUITE_SPEC =
            new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .cipherSuites(INFURA_CIPHER_SUITES)
                    .build();

    /**
     * The list of {@link ConnectionSpec} instances used by the connection.
     */
    private static final List<ConnectionSpec> CONNECTION_SPEC_LIST =
            Arrays.asList(INFURA_CIPHER_SUITE_SPEC, CLEARTEXT);

    public static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final MediaType MEDIA_TYPE_TEXT =
            MediaType.parse("text/xml; charset=UTF-8");

    public static final String DEFAULT_URL = "http://localhost:8545/";

    private static final Logger log = LoggerFactory.getLogger(org.web3j.protocol.http.HttpService.class);

    private final OkHttpClient httpClient;

    private final String[] urls; // Changed to array of URLs
    private final boolean includeRawResponse;
    private final String infuraSecret;
    private final String infuraKey;
    private final String klaytnKey;
    private final long chainId;
    private final Random random = new Random();

    private final HashMap<String, String> headers = new HashMap<>();

    public AWHttpServiceWaterfall(String[] urls, long chainId, OkHttpClient httpClient, String infuraKey, String infuraSecret, String klaytnKey, boolean includeRawResponses)
    {
        super(includeRawResponses);
        this.urls = urls;
        this.httpClient = httpClient;
        this.includeRawResponse = includeRawResponses;
        this.infuraKey = infuraKey;
        this.infuraSecret = infuraSecret;
        this.klaytnKey = klaytnKey;
        this.chainId = chainId;
    }

    @Override
    protected InputStream performIO(String request) throws IOException
    {
        // Start at a random point within the URLs
        int startIndex = random.nextInt(urls.length);

        // Round-robin try all URLs
        for (int count = 0; count < urls.length; count++)
        {
            String url = urls[(startIndex + count) % urls.length];

            try
            {
                okhttp3.Response response = performSingleIO(url, request);

                // Check if the response is valid (2xx status code)
                if (response.isSuccessful())
                {
                    return processResponse(response);
                }
                else
                {
                    Timber.d("Response was %s, retrying...", response.code());
                }
            }
            catch (IOException e)
            {
                log.warn("Request to {} failed: {}", url, e.getMessage());
                // Optionally, re-throw for specific error handling at a higher level
            }
        }

        throw new IOException("All requests failed!");
    }

    private okhttp3.Response performSingleIO(String url, String request) throws IOException
    {
        RequestBody requestBody;
        try
        {
            requestBody = RequestBody.create(request, JSON_MEDIA_TYPE);
        }
        catch (JsonParseException e)
        {
            requestBody = RequestBody.create("", MEDIA_TYPE_TEXT);
        }

        addRequiredSecrets(url);

        Request httpRequest = new Request.Builder()
                .url(url)
                .headers(buildHeaders())
                .post(requestBody)
                .build();

        return httpClient.newCall(httpRequest).execute();
    }

    private InputStream processResponse(Response response) throws IOException
    {
        processHeaders(response.headers());

        if (response.isSuccessful())
        {
            if (response.body() != null)
            {
                return buildInputStream(response);
            }
            else
            {
                return buildNullInputStream();
            }
        }
        else
        {
            throw new IOException("Unsuccessful response: " + response.code());
        }
    }


    protected void processHeaders(Headers headers)
    {
        // Default implementation is empty
    }

    private InputStream buildNullInputStream()
    {
        String jsonData = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x\"}";
        return new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream buildInputStream(Response response) throws IOException
    {
        ResponseBody responseBody = response.body();
        InputStream inputStream = validateAndGetStream(responseBody);
        //InputStream inputStream = responseBody.byteStream();

        if (includeRawResponse)
        {
            // we have to buffer the entire input payload, so that after processing
            // it can be re-read and used to populate the rawResponse field.

            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body
            Buffer buffer = source.getBuffer();

            long size = buffer.size();
            if (size > Integer.MAX_VALUE)
            {
                throw new UnsupportedOperationException(
                        "Non-integer input buffer size specified: " + size);
            }

            int bufferSize = (int) size;
            BufferedInputStream bufferedinputStream =
                    new BufferedInputStream(inputStream, bufferSize);

            bufferedinputStream.mark(inputStream.available());
            return bufferedinputStream;

        }
        else
        {
            return inputStream;
        }
    }

    private void addRequiredSecrets(String url)
    {
        if (!TextUtils.isEmpty(infuraKey) && url.endsWith(infuraKey) && !TextUtils.isEmpty(infuraSecret)) //primary InfuraKey has secret
        {
            addHeader("Authorization", "Basic " + infuraSecret);
        }
        else if (!TextUtils.isEmpty(klaytnKey) && (chainId == KLAYTN_BAOBAB_ID || chainId == KLAYTN_ID))
        {
            addHeader("x-chain-id", Long.toString(chainId));
            addHeader("Authorization", "Basic " + klaytnKey);
        }
    }

    private Headers buildHeaders()
    {
        return Headers.of(headers);
    }

    public void addHeader(String key, String value)
    {
        headers.put(key, value);
    }

    public void addHeaders(Map<String, String> headersToAdd)
    {
        headers.putAll(headersToAdd);
    }

    public HashMap<String, String> getHeaders()
    {
        return headers;
    }

    @Override
    public void close() throws IOException
    {
    }
}
