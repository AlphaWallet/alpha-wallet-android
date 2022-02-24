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
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Call;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.Signable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Web3View extends WebView {
    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "AlphaWallet.executeCallback(%1$s, null, \"%2$s\")";
    private static final String JS_PROTOCOL_ON_FAILURE = "AlphaWallet.executeCallback(%1$s, \"%2$s\", null)";

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
    private OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener;
    @Nullable
    private OnVerifyListener onVerifyListener;
    @Nullable
    private OnGetBalanceListener onGetBalanceListener;
    @Nullable
    private OnWalletActionListener onWalletActionListener;

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
        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadUrl(@NonNull String url)
    {
        loadUrl(url, getWeb3Headers());
    }

    /* Required for CORS requests */
    @NotNull
    @Contract(" -> new")
    private Map<String, String> getWeb3Headers()
    {
        //headers
        return new HashMap<String, String>() {{
            put("Connection", "close");
            put("Content-Type", "text/plain");
            put("Access-Control-Allow-Origin", "*");
            put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            put("Access-Control-Max-Age", "600");
            put("Access-Control-Allow-Credentials", "true");
            put("Access-Control-Allow-Headers", "accept, authorization, Content-Type");
        }};
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
                innerOnEthCallListener,
                innerAddChainListener,
                innerOnWalletActionListener), "alpha");
    }

    public void setWalletAddress(@NonNull Address address) {
        webViewClient.getJsInjectorClient().setWalletAddress(address);
    }

    @Nullable
    public Address getWalletAddress() {
        return webViewClient.getJsInjectorClient().getWalletAddress();
    }

    public void setChainId(long chainId) {
        webViewClient.getJsInjectorClient().setChainId(chainId);
    }

    public long getChainId() {
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

    public void setOnWalletAddEthereumChainObjectListener(@Nullable OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener) {
        this.onWalletAddEthereumChainObjectListener = onWalletAddEthereumChainObjectListener;
    }

    public void setOnWalletActionListener(@Nullable OnWalletActionListener onWalletActionListener) {
        this.onWalletActionListener = onWalletActionListener;
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
        post(() -> evaluateJavascript(callback, value ->Timber.tag("WEB_VIEW").d(value)));
    }

    public void onWalletActionSuccessful(long callbackId, String message) {
        String callback = String.format(JS_PROTOCOL_ON_SUCCESSFUL, callbackId, message);
        post(() -> {
            evaluateJavascript(callback, Timber::d);
        });
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

    private final OnWalletAddEthereumChainObjectListener innerAddChainListener = new OnWalletAddEthereumChainObjectListener()
    {
        @Override
        public void onWalletAddEthereumChainObject(long callbackId, WalletAddEthereumChainObject chainObject)
        {
            onWalletAddEthereumChainObjectListener.onWalletAddEthereumChainObject(callbackId, chainObject);
        }
    };

    private final OnWalletActionListener innerOnWalletActionListener = new OnWalletActionListener() {
        @Override
        public void onRequestAccounts(long callbackId)
        {
            onWalletActionListener.onRequestAccounts(callbackId);
        }

        @Override
        public void onWalletSwitchEthereumChain(long callbackId, WalletAddEthereumChainObject chainObj)
        {
            onWalletActionListener.onWalletSwitchEthereumChain(callbackId, chainObj);
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

    public void resetView()
    {
        webViewClient.resetInject();
    }

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
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            clearCache(true);
            if (!redirect)
            {
                view.evaluateJavascript(internalClient.getProviderString(view), null);
                view.evaluateJavascript(internalClient.getInitString(view), null);
                internalClient.resetInject();
            }

            redirect = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (!redirect && !loadingError)
            {
                if (loadInterface != null) { loadInterface.onWebpageLoaded(url, view.getTitle()); }
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

            return externalClient.shouldOverrideUrlLoading(view, request)
                    || internalClient.shouldOverrideUrlLoading(view, request);
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
