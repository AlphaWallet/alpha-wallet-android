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

    private static final String TAG = "JsInjectorClient";
    private static final String DEFAULT_CHARSET = "utf-8";
    private static final String DEFAULT_MIME_TYPE = "text/html";
    private final static String JS_TAG_TEMPLATE = "<script type=\"text/javascript\">%1$s%2$s</script>";
    final String SCRIPT_TAG = "<script";
    final String CDATA_TAG = "<![cdata[";

    /**
     * Characters that must never appear in an RPC URL we splice into a JS string
     * literal. Their presence is a strong indicator of an attempted
     * universal-XSS injection (see issue #2686).
     */
    private static final Pattern UNSAFE_URL_CHARS =
            Pattern.compile("[\"'\\\\<>\\r\\n\\t\\u0000-\\u001f]");

    private long chainId;
    private Address walletAddress;

    private String rpcUrl;

    public JsInjectorClient(Context context) {

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

    public void setChainId(long chainId)
    {
        this.chainId = chainId;
        this.rpcUrl = sanitizeRpcUrl(EthereumNetworkRepository.getDefaultNodeURL(chainId));
    }

    // Set ChainId for TokenScript inject
    public void setTSChainId(long chainId)
    {
        this.chainId = chainId;
        this.rpcUrl = sanitizeRpcUrl(EthereumNetworkRepository.getDefaultNodeURL(chainId));
    }

    /**
     * Validate and normalize an RPC URL before it is spliced into the
     * JavaScript that is injected into every loaded page. Returns an empty
     * string when the URL is missing, unparseable, not http(s), or contains
     * any character that has meaning inside a JS string literal. This blocks
     * the UXSS attack described in issue #2686, where a malicious chain
     * registered via {@code wallet_addEthereumChain} could break out of the
     * RPC-URL string in the template and inject arbitrary JavaScript.
     */
    static String sanitizeRpcUrl(String url)
    {
        if (TextUtils.isEmpty(url))
        {
            return "";
        }
        if (UNSAFE_URL_CHARS.matcher(url).find())
        {
            Log.w(TAG, "Rejecting RPC URL containing characters unsafe for JS string injection");
            return "";
        }
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null)
        {
            Log.w(TAG, "Rejecting unparseable RPC URL");
            return "";
        }
        String scheme = parsed.scheme();
        if (!"http".equals(scheme) && !"https".equals(scheme))
        {
            Log.w(TAG, "Rejecting RPC URL with non-http(s) scheme: " + scheme);
            return "";
        }
        // Re-serialize through HttpUrl so any odd encodings are normalized to a
        // canonical form before injection. Re-check the normalized form for
        // unsafe characters in case parsing decoded any.
        String normalized = parsed.toString();
        if (UNSAFE_URL_CHARS.matcher(normalized).find())
        {
            Log.w(TAG, "Rejecting normalized RPC URL containing unsafe characters");
            return "";
        }
        return normalized;
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
        initSrc = String.format(initSrc, tokenContent, walletAddress, rpcUrl, chainId, tokenIdWrapperName);
        //now insert this source into the view
        // note that the <div> is not closed because it is closed in injectStyleAndWrap().
        String ethersMin = "<script>" + loadFile(ctx, R.raw.ethers_js_min) + "</script>";
        String wrapper = "<div id=\"token-card-" + tokenId.toString(10) + "\" class=\"token-card\">";
        initSrc = ethersMin + "<script>\n" + initSrc + "</script>\n" + wrapper;
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

    String injectJSAtScriptEnd(String view, String newCode)
    {
        int position = getEndScriptPosition(view);
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

    private int getEndInjectionPosition(String body)
    {
        body = body.toLowerCase();
        int firstIndex = body.indexOf(SCRIPT_TAG);
        int nextIndex = body.indexOf("web3", firstIndex);
        return body.indexOf("</script", nextIndex);
    }

    private int getEndScriptPosition(String body)
    {
        //<script type="text/javascript">//<![CDATA[
        body = body.toLowerCase();
        int scriptTag = body.indexOf(SCRIPT_TAG) + SCRIPT_TAG.length();
        int endTag = body.indexOf(">", scriptTag) + 1;
        int endCData = body.indexOf(CDATA_TAG, endTag) + CDATA_TAG.length();
        if (endCData > -1)
        {
            return endCData;
        }
        else
        {
            return endTag;
        }
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
        return String.format(initSrc, address, rpcUrl == null ? "" : rpcUrl, chainId);
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
}
