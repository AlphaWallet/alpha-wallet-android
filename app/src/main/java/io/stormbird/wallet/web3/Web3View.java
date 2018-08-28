package io.stormbird.wallet.web3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.Transaction;
import io.stormbird.wallet.web3.entity.TypedData;

public class Web3View extends WebView {
    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "onSignSuccessful(%1$s, \"%2$s\")";
    private static final String JS_PROTOCOL_ON_FAILURE = "onSignError(%1$s, \"%2$s\")";
    @Nullable
    private OnSignTransactionListener onSignTransactionListener;
    @Nullable
    private OnSignMessageListener onSignMessageListener;
    @Nullable
    private OnSignPersonalMessageListener onSignPersonalMessageListener;
    @Nullable
    private OnSignTypedMessageListener onSignTypedMessageListener;
    @Nullable
    private OnVerifyListener onVerifyListener;
    @Nullable
    private OnGetBalanceListener onGetBalanceListener;
    private JsInjectorClient jsInjectorClient;
    private Web3ViewClient webViewClient;

    public Web3View(@NonNull Context context) {
        super(context);
        init();
    }

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Web3View(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        super.setWebChromeClient(client);
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        super.setWebViewClient(new WrapWebViewClient(webViewClient, client, jsInjectorClient));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        jsInjectorClient = new JsInjectorClient(getContext());
        webViewClient = new Web3ViewClient(jsInjectorClient, new UrlHandlerManager());
        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        addJavascriptInterface(new SignCallbackJSInterface(
                this,
                innerOnSignTransactionListener,
                innerOnSignMessageListener,
                innerOnSignPersonalMessageListener,
                innerOnSignTypedMessageListener,
                innerOnVerifyListener,
                innerOnGetBalanceListener), "trust");

        super.setWebViewClient(webViewClient);
    }

//    @Override
//    public WebSettings getSettings() {
//        return new WrapWebSettings(super.getSettings());
//    }


    public void setWalletAddress(@NonNull Address address) {
        jsInjectorClient.setWalletAddress(address);
    }

    @Nullable
    public Address getWalletAddress() {
        return jsInjectorClient.getWalletAddress();
    }

    public void setChainId(int chainId) {
        jsInjectorClient.setChainId(chainId);
    }

    public int getChainId() {
        return jsInjectorClient.getChainId();
    }

    public void setRpcUrl(@NonNull String rpcUrl) {
        jsInjectorClient.setRpcUrl(rpcUrl);
    }

    @Nullable
    public String getRpcUrl() {
        return jsInjectorClient.getRpcUrl();
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

    public void setOnVerifyListener(@Nullable OnVerifyListener onVerifyListener) {
        this.onVerifyListener = onVerifyListener;
    }

    public void setOnGetBalanceListener(@Nullable OnGetBalanceListener onGetBalanceListener) {
        this.onGetBalanceListener = onGetBalanceListener;
    }

    public void onSignTransactionSuccessful(Transaction transaction, String signHex) {
        long callbackId = transaction.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onSignMessageSuccessful(Message message, String signHex) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onSignPersonalMessageSuccessful(Message message, String signHex) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void onSignError(Transaction transaction, String error) {
        long callbackId = transaction.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignError(Message message, String error) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, error);
    }

    public void onSignCancel(Transaction transaction) {
        long callbackId = transaction.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    public void onSignCancel(Message message) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    public void onVerify(String recoveredAddress, String result) {
        post(() -> {
            loadUrl("javascript:(function() {" +
                    "alert('" + result + "');" +
                    "verificationAddressBox.value = '" + recoveredAddress + "';" +
                    "})()");
        });
    }

    public void onGetBalance(String balance) {
        post(() -> {
            loadUrl("javascript:(function() {" +
                    "var balanceDiv = document.createElement('div');" +
                    "balanceDiv.style.cssText = 'color:white';" +
                    "balanceDiv.innerHTML = 'Wallet Balance: " + balance + "';" +
                    "document.getElementsByClassName('container')[0].appendChild(balanceDiv);" +
                    "})()");
        });
    }

    private void callbackToJS(long callbackId, String function, String param) {
        String callback = String.format(function, callbackId, param);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            post(() -> evaluateJavascript(callback, value -> Log.d("WEB_VIEW", value)));
        }
    }

    private final OnSignTransactionListener innerOnSignTransactionListener = new OnSignTransactionListener() {
        @Override
        public void onSignTransaction(Transaction transaction) {
            if (onSignTransactionListener != null) {
                onSignTransactionListener.onSignTransaction(transaction);
            }
        }
    };

    private final OnSignMessageListener innerOnSignMessageListener = new OnSignMessageListener() {
        @Override
        public void onSignMessage(Message message) {
            if (onSignMessageListener != null) {
                onSignMessageListener.onSignMessage(message);
            }
        }
    };

    private final OnSignPersonalMessageListener innerOnSignPersonalMessageListener = new OnSignPersonalMessageListener() {
        @Override
        public void onSignPersonalMessage(Message message) {
            onSignPersonalMessageListener.onSignPersonalMessage(message);
        }
    };

    private final OnSignTypedMessageListener innerOnSignTypedMessageListener = new OnSignTypedMessageListener() {
        @Override
        public void onSignTypedMessage(Message<TypedData[]> message) {
            onSignTypedMessageListener.onSignTypedMessage(message);
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
        private final JsInjectorClient jsInjectorClient;

        public WrapWebViewClient(Web3ViewClient internalClient, WebViewClient externalClient, JsInjectorClient jsInjectorClient) {
            this.internalClient = internalClient;
            this.externalClient = externalClient;
            this.jsInjectorClient = jsInjectorClient;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            /* Inject javascript to override verify button onclick */
            view.loadUrl("javascript:(function() { " +
                    "var messageBox = document.getElementById('messageBox');" +
                    "var verifyMessageBox = document.getElementById('verifyMessageBox');" +
                    "var signatureBox = document.getElementById('signatureBox');" +
                    "var verificationAddressBox = document.getElementById('verificationAddressBox');" +
                    "var verifyBtn = document.getElementById('verifyButton');" +
                    "verifyBtn.onclick = function(){ " +
                    "trust.verify(verifyMessageBox.value, signatureBox.value); " +
                    "};" +
                    // Get Balance
                    "var account = web3.eth.accounts[0];" +
                    "var walletBalance = web3.eth.getBalance(account, " +
                    "function(error, result) { " +
                    "if (error) alert(error);" +
                    "trust.getBalance(result.toString());" +
                    "});" +
                    "})()");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return externalClient.shouldOverrideUrlLoading(view, url)
                    || internalClient.shouldOverrideUrlLoading(view, url);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return externalClient.shouldOverrideUrlLoading(view, request)
                    || internalClient.shouldOverrideUrlLoading(view, request);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                    String injectedHtml = jsInjectorClient.injectJS(new String(data));
                    response.setData(new ByteArrayInputStream(injectedHtml.getBytes()));
                } catch (IOException ex) {
                    Log.d("INJECT AFTER_EXTRNAL", "", ex);
                }
            } else {
                response = internalClient.shouldInterceptRequest(view, request);
            }
            return response;
        }
    }
}
