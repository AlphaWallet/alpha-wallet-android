package io.stormbird.wallet.web3;

import android.net.http.SslError;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.util.Map;

import okhttp3.HttpUrl;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

public class Web3ViewClient extends WebViewClient {

    private final Object lock = new Object();

    private final JsInjectorClient jsInjectorClient;
    private final UrlHandlerManager urlHandlerManager;

    private boolean isInjected;

    public Web3ViewClient(JsInjectorClient jsInjectorClient, UrlHandlerManager urlHandlerManager) {
        this.jsInjectorClient = jsInjectorClient;
        this.urlHandlerManager = urlHandlerManager;
    }

    void addUrlHandler(UrlHandler urlHandler) {
        urlHandlerManager.add(urlHandler);
    }

    void removeUrlHandler(UrlHandler urlHandler) {
        urlHandlerManager.remove(urlHandler);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return shouldOverrideUrlLoading(view, url, false, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (request == null || view == null) {
            return false;
        }
        String url = request.getUrl().toString();
        boolean isMainFrame = request.isForMainFrame();
        boolean isRedirect = SDK_INT >= N && request.isRedirect();
        return shouldOverrideUrlLoading(view, url, isMainFrame, isRedirect);
    }

    private boolean shouldOverrideUrlLoading(WebView webView, String url, boolean isMainFrame, boolean isRedirect) {
        boolean result = false;
        synchronized (lock) {
            isInjected = false;
        }
        String urlToOpen = urlHandlerManager.handle(url);
        if (!url.startsWith("http")) {
            result = true;
        }
        if (isMainFrame && isRedirect) {
            urlToOpen = url;
            result = true;
        }

        if (result && !TextUtils.isEmpty(urlToOpen)) {
            webView.loadUrl(urlToOpen);
        }
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request == null) {
            return null;
        }
        if (!request.getMethod().equalsIgnoreCase("GET") || !request.isForMainFrame()) {
             if (request.getMethod().equalsIgnoreCase("GET")
                     && (request.getUrl().toString().contains(".js")
                        || request.getUrl().toString().contains("json")
                        || request.getUrl().toString().contains("css"))) {
                synchronized (lock) {
                    if (!isInjected) {
                        injectScriptFile(view);
                        isInjected = true;
                    }
                }
            }
            super.shouldInterceptRequest(view, request);
            return null;
        }

        HttpUrl httpUrl = HttpUrl.parse(request.getUrl().toString());
        if (httpUrl == null) {
            return null;
        }
        Map<String, String> headers = request.getRequestHeaders();
        JsInjectorResponse response;
        try {
            response = jsInjectorClient.loadUrl(httpUrl.toString(), headers);
        } catch (Exception ex) {
            return null;
        }
        if (response == null || response.isRedirect) {
            return null;
        } else if (TextUtils.isEmpty(response.data)){
            return null;
        } else {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(response.data.getBytes());
            WebResourceResponse webResourceResponse = new WebResourceResponse(
                    response.mime, response.charset, inputStream);
            synchronized (lock) {
                isInjected = true;
            }
            return webResourceResponse;
        }
    }

    private void injectScriptFile(WebView view) {
        String js = jsInjectorClient.assembleJs(view.getContext(), "%1$s%2$s");
        byte[] buffer = js.getBytes();
        String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);

        view.post(() -> view.loadUrl("javascript:(function() {" +
                "var parent = document.getElementsByTagName('head').item(0);" +
                "var script = document.createElement('script');" +
                "script.type = 'text/javascript';" +
                // Tell the browser to BASE64-decode the string into your script !!!
                "script.innerHTML = window.atob('" + encoded + "');" +
                "parent.appendChild(script)" +
                "})()"));
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed();
    }

    public void onReload() {
        synchronized (lock) {
            isInjected = false;
        }
    }
}