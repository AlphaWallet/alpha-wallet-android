package com.alphawallet.app.web3;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.web3.entity.Address;

import org.web3j.crypto.Keys;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.alphawallet.app.util.Utils.loadFile;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class JsInjectorClient {

    private static final String DEFAULT_CHARSET = "utf-8";
    private static final String DEFAULT_MIME_TYPE = "text/html";
    private final static String JS_TAG_TEMPLATE = "<script type=\"text/javascript\">%1$s%2$s</script>";

    private final Context context;
    private final OkHttpClient httpClient;

    private long chainId = 1;
    private Address walletAddress;
    //Note: this default RPC is overriden before injection
    private String rpcUrl = EthereumNetworkRepository.getDefaultNodeURL(MAINNET_ID);

    public JsInjectorClient(Context context) {
        this.context = context;
        this.httpClient = createHttpClient();
    }

    public Address getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(Address address) {
        this.walletAddress = address;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String initJs(Context context)
    {
        return loadInitJs(context);
    }

    public String providerJs(Context context)
    {
        return loadFile(context, R.raw.alphawallet_min);
    }

    String injectWeb3TokenInit(Context ctx, String view, String tokenContent, BigInteger tokenId)
    {
        String initSrc = loadFile(ctx, R.raw.init_token);
        //put the view in here
        String tokenIdWrapperName = "token-card-" + tokenId.toString(10);
        initSrc = String.format(initSrc, tokenContent, walletAddress, EthereumNetworkRepository.getDefaultNodeURL(chainId), chainId, tokenIdWrapperName);
        //now insert this source into the view
        // note that the <div> is not closed because it is closed in injectStyleAndWrap().
        String wrapper = "<div id=\"token-card-" + tokenId.toString(10) + "\" class=\"token-card\">";
        initSrc = "<script>\n" + initSrc + "</script>\n" + wrapper;
        return injectJS(view, initSrc);
    }

    String injectJSAtEnd(String view, String newCode)
    {
        int position = getEndInjectionPosition(view);
        if (position >= 0) {
            String beforeTag = view.substring(0, position);
            String afterTab = view.substring(position);
            return beforeTag + newCode + afterTab;
        }
        return view;
    }

    String injectJS(String html, String js) {
        if (TextUtils.isEmpty(html)) {
            return html;
        }
        int position = getInjectionPosition(html);
        if (position >= 0) {
            String beforeTag = html.substring(0, position);
            String afterTab = html.substring(position);
            return beforeTag + js + afterTab;
        }
        return html;
    }

    private int getInjectionPosition(String body) {
        body = body.toLowerCase();
        int ieDetectTagIndex = body.indexOf("<!--[if");
        int scriptTagIndex = body.indexOf("<script");

        int index;
        if (ieDetectTagIndex < 0) {
            index = scriptTagIndex;
        } else {
            index = Math.min(scriptTagIndex, ieDetectTagIndex);
        }
        if (index < 0) {
            index = body.indexOf("</head");
        }
        if (index < 0) {
            index = 0; //just wrap whole view
        }
        return index;
    }

    private int getEndInjectionPosition(String body) {
        body = body.toLowerCase();
        int firstIndex = body.indexOf("<script");
        int nextIndex = body.indexOf("web3", firstIndex);
        return body.indexOf("</script", nextIndex);
    }

    @Nullable
    private Request buildRequest(String url, Map<String, String> headers) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            return null;
        }
        Request.Builder requestBuilder = new Request.Builder()
                .get()
                .url(httpUrl);
        Set<String> keys = headers.keySet();
        for (String key : keys) {
            requestBuilder.addHeader(key, headers.get(key));
        }
        return requestBuilder.build();
    }

    private String loadInitJs(Context context) {
        String initSrc = loadFile(context, R.raw.init);
        String address = walletAddress == null ? Address.EMPTY.toString() : Keys.toChecksumAddress(walletAddress.toString());
        return String.format(initSrc, address, rpcUrl, chainId);
    }

    String injectStyleAndWrap(String view, String style)
    {
        if (style == null) style = "";
        //String injectHeader = "<head><meta name=\"viewport\" content=\"width=device-width, user-scalable=false\" /></head>";
        String injectHeader = "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, shrink-to-fit=no\" />"; //iOS uses these header settings
        style = "<style type=\"text/css\">\n" + style + ".token-card {\n" +
                "padding: 0pt;\n" +
                "margin: 0pt;\n" +
                "}</style></head>" +
                "<body>\n";
        // the opening of the following </div> is in injectWeb3TokenInit();
        return injectHeader + style + view + "</div></body>";
    }

    private String getMimeType(String contentType) {
        Matcher regexResult = Pattern.compile("^.*(?=;)").matcher(contentType);
        if (regexResult.find()) {
            return regexResult.group();
        }
        return DEFAULT_MIME_TYPE;
    }

    private String getCharset(String contentType) {
        Matcher regexResult = Pattern.compile("charset=([a-zA-Z0-9-]+)").matcher(contentType);
        if (regexResult.find()) {
            if (regexResult.groupCount() >= 2) {
                return regexResult.group(1);
            }
        }
        return DEFAULT_CHARSET;
    }

    @Nullable
    private String getContentTypeHeader(Response response) {
        Headers headers = response.headers();
        String contentType;
        if (TextUtils.isEmpty(headers.get("Content-Type"))) {
            if (TextUtils.isEmpty(headers.get("content-Type"))) {
                contentType = "text/data; charset=utf-8";
            } else {
                contentType = headers.get("content-Type");
            }
        } else {
            contentType = headers.get("Content-Type");
        }
        if (contentType != null) {
            contentType = contentType.trim();
        }
        return contentType;
    }

    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .cookieJar(new WebViewCookieJar())
                .build();
    }
}
