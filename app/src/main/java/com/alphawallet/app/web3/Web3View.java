package com.alphawallet.app.web3;

import static androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF;
import static androidx.webkit.WebSettingsCompat.FORCE_DARK_ON;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

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

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Web3View extends WebView {
    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "AlphaWallet.executeCallback(%1$s, null, \"%2$s\")";
    private static final String JS_PROTOCOL_EXPR_ON_SUCCESSFUL = "AlphaWallet.executeCallback(%1$s, null, %2$s)";
    private static final String JS_PROTOCOL_ON_FAILURE = "AlphaWallet.executeCallback(%1$s, \"%2$s\", null)";
    private final Web3ViewClient webViewClient;
    @Nullable
    private OnSignTransactionListener onSignTransactionListener;
    private final OnSignTransactionListener innerOnSignTransactionListener = new OnSignTransactionListener() {
        @Override
        public void onSignTransaction(Web3Transaction transaction, String url)
        {
            if (onSignTransactionListener != null)
            {
                onSignTransactionListener.onSignTransaction(transaction, url);
            }
        }
    };
    @Nullable
    private OnSignMessageListener onSignMessageListener;
    private final OnSignMessageListener innerOnSignMessageListener = new OnSignMessageListener() {
        @Override
        public void onSignMessage(EthereumMessage message)
        {
            if (onSignMessageListener != null)
            {
                onSignMessageListener.onSignMessage(message);
            }
        }
    };
    @Nullable
    private OnSignPersonalMessageListener onSignPersonalMessageListener;
    private final OnSignPersonalMessageListener innerOnSignPersonalMessageListener = new OnSignPersonalMessageListener() {
        @Override
        public void onSignPersonalMessage(EthereumMessage message)
        {
            onSignPersonalMessageListener.onSignPersonalMessage(message);
        }
    };
    @Nullable
    private OnSignTypedMessageListener onSignTypedMessageListener;
    private final OnSignTypedMessageListener innerOnSignTypedMessageListener = new OnSignTypedMessageListener() {
        @Override
        public void onSignTypedMessage(EthereumTypedMessage message)
        {
            onSignTypedMessageListener.onSignTypedMessage(message);
        }
    };
    @Nullable
    private OnEthCallListener onEthCallListener;
    private final OnEthCallListener innerOnEthCallListener = new OnEthCallListener() {
        @Override
        public void onEthCall(Web3Call txData)
        {
            onEthCallListener.onEthCall(txData);
        }
    };
    @Nullable
    private OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener;
    private final OnWalletAddEthereumChainObjectListener innerAddChainListener = new OnWalletAddEthereumChainObjectListener() {
        @Override
        public void onWalletAddEthereumChainObject(long callbackId, WalletAddEthereumChainObject chainObject)
        {
            onWalletAddEthereumChainObjectListener.onWalletAddEthereumChainObject(callbackId, chainObject);
        }
    };
    @Nullable
    private OnWalletActionListener onWalletActionListener;
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
    private URLLoadInterface loadInterface;

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        webViewClient = new Web3ViewClient(getContext());
        init();
    }

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        webViewClient = new Web3ViewClient(getContext());
        init();
    }

    @Override
    public void setWebChromeClient(WebChromeClient client)
    {
        super.setWebChromeClient(client);
    }

    @Override
    public void setWebViewClient(WebViewClient client)
    {
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
    public void init()
    {
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
        
//        Removing this block for now.
//        TODO: Figure out if we should support dark mode for external websites
//        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
//        {
//            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
//            {
//                case Configuration.UI_MODE_NIGHT_YES:
//                    WebSettingsCompat.setForceDark(getSettings(), FORCE_DARK_ON);
//                    break;
//                case Configuration.UI_MODE_NIGHT_NO:
//                case Configuration.UI_MODE_NIGHT_UNDEFINED:
//                    WebSettingsCompat.setForceDark(getSettings(), FORCE_DARK_OFF);
//                    break;
//            }
//        }
    }

    @Nullable
    public Address getWalletAddress()
    {
        return webViewClient.getJsInjectorClient().getWalletAddress();
    }

    public void setWalletAddress(@NonNull Address address)
    {
        webViewClient.getJsInjectorClient().setWalletAddress(address);
    }

    public long getChainId()
    {
        return webViewClient.getJsInjectorClient().getChainId();
    }

    public void setChainId(long chainId)
    {
        webViewClient.getJsInjectorClient().setChainId(chainId);
    }

    public void setWebLoadCallback(URLLoadInterface iFace)
    {
        loadInterface = iFace;
    }

    public void setRpcUrl(@NonNull String rpcUrl)
    {
        webViewClient.getJsInjectorClient().setRpcUrl(rpcUrl);
    }

    public void setOnSignTransactionListener(@Nullable OnSignTransactionListener onSignTransactionListener)
    {
        this.onSignTransactionListener = onSignTransactionListener;
    }

    public void setOnSignMessageListener(@Nullable OnSignMessageListener onSignMessageListener)
    {
        this.onSignMessageListener = onSignMessageListener;
    }

    public void setOnSignPersonalMessageListener(@Nullable OnSignPersonalMessageListener onSignPersonalMessageListener)
    {
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
    }

    public void setOnSignTypedMessageListener(@Nullable OnSignTypedMessageListener onSignTypedMessageListener)
    {
        this.onSignTypedMessageListener = onSignTypedMessageListener;
    }

    public void setOnEthCallListener(@Nullable OnEthCallListener onEthCallListener)
    {
        this.onEthCallListener = onEthCallListener;
    }

    public void setOnWalletAddEthereumChainObjectListener(@Nullable OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener)
    {
        this.onWalletAddEthereumChainObjectListener = onWalletAddEthereumChainObjectListener;
    }

    public void setOnWalletActionListener(@Nullable OnWalletActionListener onWalletActionListener)
    {
        this.onWalletActionListener = onWalletActionListener;
    }

    public void onSignTransactionSuccessful(Web3Transaction transaction, String signHex)
    {
        long callbackId = transaction.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onSignMessageSuccessful(Signable message, String signHex)
    {
        long callbackId = message.getCallbackId();
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onCallFunctionSuccessful(long callbackId, String result)
    {
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, result);
    }

    public void onCallFunctionError(long callbackId, String error)
    {
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignCancel(long callbackId)
    {
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    private void callbackToJS(long callbackId, String function, String param)
    {
        String callback = String.format(function, callbackId, param);
        post(() -> evaluateJavascript(callback, value ->Timber.tag("WEB_VIEW").d(value)));
    }

    public void onWalletActionSuccessful(long callbackId, String expression)
    {
        String callback = String.format(JS_PROTOCOL_EXPR_ON_SUCCESSFUL, callbackId, expression);
        post(() -> evaluateJavascript(callback, Timber::d));
    }

    public void resetView()
    {
        webViewClient.resetInject();
    }

    private class WrapWebViewClient extends WebViewClient {
        private final Web3ViewClient internalClient;
        private final WebViewClient externalClient;
        private boolean loadingError = false;
        private boolean redirect = false;

        public WrapWebViewClient(Web3ViewClient internalClient, WebViewClient externalClient)
        {
            this.internalClient = internalClient;
            this.externalClient = externalClient;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
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
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);

            if (!redirect && !loadingError)
            {
                if (loadInterface != null)
                {
                    loadInterface.onWebpageLoaded(url, view.getTitle());
                }
            }
            else if (!loadingError && loadInterface != null)
            {
                loadInterface.onWebpageLoadComplete();
            }

            redirect = false;
            loadingError = false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            redirect = true;

            return externalClient.shouldOverrideUrlLoading(view, url)
                    || internalClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
        {
            loadingError = true;
            if (externalClient != null)
                externalClient.onReceivedError(view, request, error);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
        {
            redirect = true;

            return externalClient.shouldOverrideUrlLoading(view, request)
                    || internalClient.shouldOverrideUrlLoading(view, request);
        }
    }
}
