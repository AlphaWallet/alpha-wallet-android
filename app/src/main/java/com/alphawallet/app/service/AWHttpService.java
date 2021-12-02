package com.alphawallet.app.service;

/**
 * Created by JB on 7/05/2020.
 *
 * This class extends Web3j's HttpService connection and provides a backup URL connection.
 * The backup connection is used if there's a timeout on the main node.
 * This class provides ALL net access to Ethereum nodes for AlphaWallet
 *
 */

import static okhttp3.ConnectionSpec.CLEARTEXT;

import com.google.gson.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.http.HttpService;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/** HTTP implementation of our services API. */
public class AWHttpService extends HttpService
{
    /** Copied from {@link ConnectionSpec#APPROVED_CIPHER_SUITES}. */
    @SuppressWarnings("JavadocReference")
    private static final CipherSuite[] INFURA_CIPHER_SUITES =
            new CipherSuite[] {
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

    /** The list of {@link ConnectionSpec} instances used by the connection. */
    private static final List<ConnectionSpec> CONNECTION_SPEC_LIST =
            Arrays.asList(INFURA_CIPHER_SUITE_SPEC, CLEARTEXT);

    public static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final MediaType MEDIA_TYPE_TEXT =
            MediaType.parse("text/xml; charset=UTF-8");

    public static final String DEFAULT_URL = "http://localhost:8545/";

    private static final Logger log = LoggerFactory.getLogger(org.web3j.protocol.http.HttpService.class);

    private final OkHttpClient httpClient;

    private final String url;
    private final String secondaryUrl;

    private final boolean includeRawResponse;

    private final HashMap<String, String> headers = new HashMap<>();

    public AWHttpService(String url, String secondaryUrl, OkHttpClient httpClient, boolean includeRawResponses) {
        super(includeRawResponses);
        this.url = url;
        this.httpClient = httpClient;
        this.includeRawResponse = includeRawResponses;
        this.secondaryUrl = secondaryUrl;
    }

    @Override
    protected InputStream performIO(String request) throws IOException
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

        Headers headers = buildHeaders();
        okhttp3.Request httpRequest =
                new okhttp3.Request.Builder().url(url).headers(headers).post(requestBody).build();

        okhttp3.Response response;

        try
        {
            //try primary node
            response = httpClient.newCall(httpRequest).execute();
        }
        catch (SocketTimeoutException e) //seamlessly attempt a call to secondary node if primary timed out
        {
            if (secondaryUrl != null) //only if we have a secondary node
            {
                return trySecondaryNode(request);
            }
            else
            {
                throw new SocketTimeoutException();
            }
        }
        catch (Exception e)
        {
            return buildNullInputStream();
        }

        if (response.code()/100 == 4) //rate limited
        {
            return trySecondaryNode(request);
        }

        return processNodeResponse(response, request, false);
    }

    private InputStream trySecondaryNode(String request) throws IOException
    {
        RequestBody requestBody = RequestBody.create(request, JSON_MEDIA_TYPE);
        Headers headers = buildHeaders();

        okhttp3.Request httpRequest =
                new okhttp3.Request.Builder().url(secondaryUrl).headers(headers).post(requestBody).build();

        okhttp3.Response response;

        try
        {
            response = httpClient.newCall(httpRequest).execute();
        }
        catch (InterruptedIOException e)
        {
            return buildNullInputStream();
        }

        return processNodeResponse(response, request, true);
    }

    private InputStream processNodeResponse(Response response, String request, boolean useSecondaryNode) throws IOException
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
                //build fake response 0x
                return buildNullInputStream();
            }
        }
        else if (!useSecondaryNode && secondaryUrl != null)
        {
            return trySecondaryNode(request);
        }
        else
        {
            int code = response.code();
            String text = response.body() == null ? "N/A" : response.body().string();
            response.close();

            throw new SocketTimeoutException("Invalid response received: " + code + "; " + text);
        }
    }

    protected void processHeaders(Headers headers) {
        // Default implementation is empty
    }

    private InputStream buildNullInputStream() {
        String jsonData = "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x\"}";
        return new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream buildInputStream(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        InputStream inputStream = responseBody.byteStream();

        if (includeRawResponse) {
            // we have to buffer the entire input payload, so that after processing
            // it can be re-read and used to populate the rawResponse field.

            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body
            Buffer buffer = source.getBuffer();

            long size = buffer.size();
            if (size > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException(
                        "Non-integer input buffer size specified: " + size);
            }

            int bufferSize = (int) size;
            BufferedInputStream bufferedinputStream =
                    new BufferedInputStream(inputStream, bufferSize);

            bufferedinputStream.mark(inputStream.available());
            return bufferedinputStream;

        } else {
            return inputStream;
        }
    }

    private Headers buildHeaders() {
        return Headers.of(headers);
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addHeaders(Map<String, String> headersToAdd) {
        headers.putAll(headersToAdd);
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void close() throws IOException {}
}
