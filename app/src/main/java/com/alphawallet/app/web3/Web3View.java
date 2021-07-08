package com.alphawallet.app.web3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.URLLoadInterface;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Call;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.Signable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Web3View extends WebView {
    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "executeCallback(%1$s, null, \"%2$s\")";
    private static final String JS_PROTOCOL_ON_FAILURE = "executeCallback(%1$s, \"%2$s\", null)";

    @Nullable
    private OnSignTransactionListener onSignTransactionListener;
    @Nullable
    private OnSignMessageListener onSignMessageListener;
    @Nullable
    private OnSignPersonalMessageListener onSignPersonalMessageListener;
    @Nullable
    private OnSignTypedMessageListener onSignTypedMessageListener;
    @Nullable
    private OnEthCallListener onEthCallListener;
    @Nullable
    private OnVerifyListener onVerifyListener;
    @Nullable
    private OnGetBalanceListener onGetBalanceListener;
    private final Web3ViewClient webViewClient;
    private URLLoadInterface loadInterface;

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        webViewClient = new Web3ViewClient(getContext());
        init();
    }

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        webViewClient = new Web3ViewClient(getContext());
        init();
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        super.setWebChromeClient(client);
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(new WrapWebViewClient(webViewClient, client));
    }

    @Override
    public void loadUrl(@NonNull String url, @NonNull Map<String, String> additionalHttpHeaders)
    {
        checkDOMUsage(url);
        super.loadUrl(url, additionalHttpHeaders);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void init() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
        getSettings().setUseWideViewPort(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        getSettings().setUserAgentString(getSettings().getUserAgentString()
                                               + "AlphaWallet(Platform=Android&AppVersion=" + BuildConfig.VERSION_NAME + ")");
        WebView.setWebContentsDebuggingEnabled(true); //so devs can debug their scripts/pages
        addJavascriptInterface(new SignCallbackJSInterface(
                this,
                innerOnSignTransactionListener,
                innerOnSignMessageListener,
                innerOnSignPersonalMessageListener,
                innerOnSignTypedMessageListener,
                innerOnEthCallListener), "alpha");
    }

    public void setWalletAddress(@NonNull Address address) {
        webViewClient.getJsInjectorClient().setWalletAddress(address);
    }

    @Nullable
    public Address getWalletAddress() {
        return webViewClient.getJsInjectorClient().getWalletAddress();
    }

    public void setChainId(int chainId) {
        webViewClient.getJsInjectorClient().setChainId(chainId);
    }

    public int getChainId() {
        return webViewClient.getJsInjectorClient().getChainId();
    }

    public void setRpcUrl(@NonNull String rpcUrl) {
        webViewClient.getJsInjectorClient().setRpcUrl(rpcUrl);
    }

    public void setWebLoadCallback(URLLoadInterface iFace)
    {
        loadInterface = iFace;
    }

    @Nullable
    public String getRpcUrl() {
        return webViewClient.getJsInjectorClient().getRpcUrl();
    }

    public void addUrlHandler(@NonNull UrlHandler urlHandler) {
        webViewClient.addUrlHandler(urlHandler);
    }

    public void removeUrlHandler(@NonNull UrlHandler urlHandler) {
        webViewClient.removeUrlHandler(urlHandler);
    }

    public void setOnSignTransactionListener(@Nullable OnSignTransactionListener onSignTransactionListener) {
        this.onSignTransactionListener = onSignTransactionListener;
    }

    public void setOnSignMessageListener(@Nullable OnSignMessageListener onSignMessageListener) {
        this.onSignMessageListener = onSignMessageListener;
    }

    public void setOnSignPersonalMessageListener(@Nullable OnSignPersonalMessageListener onSignPersonalMessageListener) {
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
    }

    public void setOnSignTypedMessageListener(@Nullable OnSignTypedMessageListener onSignTypedMessageListener) {
        this.onSignTypedMessageListener = onSignTypedMessageListener;
    }

    public void setOnEthCallListener(@Nullable OnEthCallListener onEthCallListener) {
        this.onEthCallListener = onEthCallListener;
    }

    public void setOnVerifyListener(@Nullable OnVerifyListener onVerifyListener) {
        this.onVerifyListener = onVerifyListener;
    }

    public void setOnGetBalanceListener(@Nullable OnGetBalanceListener onGetBalanceListener) {
        this.onGetBalanceListener = onGetBalanceListener;
    }

    public void onSignTransactionSuccessful(Web3Transaction transaction, String signHex) {
        long callbackId = transaction.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onSignMessageSuccessful(Signable message, String signHex) {
        long callbackId = message.getCallbackId();
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onCallFunctionSuccessful(long callbackId, String result) {
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, result);
    }

    public void onCallFunctionError(long callbackId, String error) {
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignError(Web3Transaction transaction, String error) {
        long callbackId = transaction.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignError(EthereumMessage message, String error) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignCancel(long callbackId) {
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    private void callbackToJS(long callbackId, String function, String param) {
        String callback = String.format(function, callbackId, param);
        post(() -> evaluateJavascript(callback, value -> Log.d("WEB_VIEW", value)));
    }

    private final OnSignTransactionListener innerOnSignTransactionListener = new OnSignTransactionListener() {
        @Override
        public void onSignTransaction(Web3Transaction transaction, String url) {
            if (onSignTransactionListener != null) {
                onSignTransactionListener.onSignTransaction(transaction, url);
            }
        }
    };

    private final OnSignMessageListener innerOnSignMessageListener = new OnSignMessageListener() {
        @Override
        public void onSignMessage(EthereumMessage message) {
            if (onSignMessageListener != null) {
                onSignMessageListener.onSignMessage(message);
            }
        }
    };

    private final OnSignPersonalMessageListener innerOnSignPersonalMessageListener = new OnSignPersonalMessageListener() {
        @Override
        public void onSignPersonalMessage(EthereumMessage message) {
            onSignPersonalMessageListener.onSignPersonalMessage(message);
        }
    };

    private final OnSignTypedMessageListener innerOnSignTypedMessageListener = new OnSignTypedMessageListener() {
        @Override
        public void onSignTypedMessage(EthereumTypedMessage message) {
            onSignTypedMessageListener.onSignTypedMessage(message);
        }
    };

    private final OnEthCallListener innerOnEthCallListener = new OnEthCallListener()
    {
        @Override
        public void onEthCall(Web3Call txData)
        {
            onEthCallListener.onEthCall(txData);
        }
    };

    private final OnVerifyListener innerOnVerifyListener = new OnVerifyListener() {
        @Override
        public void onVerify(String message, String signHex) {
            if (onVerifyListener != null) {
                onVerifyListener.onVerify(message, signHex);
            }
        }
    };

    private final OnGetBalanceListener innerOnGetBalanceListener = new OnGetBalanceListener() {
        @Override
        public void onGetBalance(String balance) {
            if (onGetBalanceListener != null) {
                onGetBalanceListener.onGetBalance(balance);
            }
        }
    };

    private class WrapWebViewClient extends WebViewClient {
        private final Web3ViewClient internalClient;
        private final WebViewClient externalClient;
        private boolean loadingError = false;
        private boolean redirect = false;

        public WrapWebViewClient(Web3ViewClient internalClient, WebViewClient externalClient) {
            this.internalClient = internalClient;
            this.externalClient = externalClient;
        }

        @Override
        public void onPageStarted(WebView view, String url,Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (!redirect && !loadingError)
            {
                if (loadInterface != null) loadInterface.onWebpageLoaded(url, view.getTitle());
            }
            else if (!loadingError && loadInterface != null)
            {
                loadInterface.onWebpageLoadComplete();
            }

            redirect = false;
            loadingError = false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            redirect = true;

            return externalClient.shouldOverrideUrlLoading(view, url)
                    || internalClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            loadingError = true;
            if (externalClient != null)
                externalClient.onReceivedError(view, request, error);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            redirect = true;
            if (!TextUtils.isEmpty(request.getUrl().toString())) { checkDOMUsage(request.getUrl().toString()); }

            return externalClient.shouldOverrideUrlLoading(view, request)
                    || internalClient.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            WebResourceResponse response = externalClient.shouldInterceptRequest(view, request);
            if (response != null) {
                try {
                    InputStream in = response.getData();
                    int len = in.available();
                    byte[] data = new byte[len];
                    int readLen = in.read(data);
                    if (readLen == 0) {
                        throw new IOException("Nothing is read.");
                    }
                    String injectedHtml = internalClient.getJsInjectorClient().injectJS(new String(data));
                    response.setData(new ByteArrayInputStream(injectedHtml.getBytes()));
                } catch (IOException ex) {
                    Log.d("INJECT AFTER_EXTERNAL", "", ex);
                }
            } else {
                response = internalClient.shouldInterceptRequest(view, request);
            }
            return response;
        }
    }

    // Grim hack for dapps that still use local storage
    private void checkDOMUsage(@NotNull String url)
    {
        if (url.equals("https://wallet.matic.network/bridge/")) // may need other sites added
        {
            getSettings().setDomStorageEnabled(false);
        }
        else
        {
            getSettings().setDomStorageEnabled(true);
        }
    }

    private static boolean isJson(String value) {
        try {
            JSONObject stateData = new JSONObject(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
