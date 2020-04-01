package com.alphawallet.app.web3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokenscript.TokenScriptRenderCallback;
import com.alphawallet.app.entity.tokenscript.WebCompletionCallback;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.FunctionCallback;
import com.alphawallet.app.web3.entity.Message;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.web3.entity.TypedData;
import com.alphawallet.app.web3.entity.Web3Transaction;

import java.math.BigInteger;

import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_ERROR;

/**
 * Created by James on 3/04/2019.
 * Stormbird in Singapore
 */
public class Web3TokenView extends WebView
{
    public static final String RENDERING_ERROR = "<html>" + TOKENSCRIPT_ERROR + "${ERR1}</html>";

    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "executeCallback(%1$s, null, \"%2$s\")";
    private static final String JS_PROTOCOL_ON_FAILURE = "executeCallback(%1$s, \"%2$s\", null)";

    private JsInjectorClient jsInjectorClient;
    private TokenScriptClient tokenScriptClient;
    private PageReadyCallback assetHolder;
    private boolean showingError = false;

    protected WebCompletionCallback keyPressCallback;

    @Nullable
    private OnSignPersonalMessageListener onSignPersonalMessageListener;

    public Web3TokenView(@NonNull Context context) {
        super(context);
        init();
    }

    public Web3TokenView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Web3TokenView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        tokenScriptClient = new TokenScriptClient(this);
        jsInjectorClient = new JsInjectorClient(getContext());
        WebSettings webSettings = super.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUserAgentString(webSettings.getUserAgentString()
                                               + "AlphaWallet(Platform=Android&AppVersion=" + BuildConfig.VERSION_NAME + ")");
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        setScrollBarSize(0);
        setVerticalScrollBarEnabled(false);
        setScrollContainer(false);
        setScrollbarFadingEnabled(true);

        setInitialScale(0);
        clearCache(true);
        showingError = false;

        addJavascriptInterface(new SignCallbackJSInterface(
                this,
                innerOnSignTransactionListener,
                innerOnSignMessageListener,
                innerOnSignPersonalMessageListener,
                innerOnSignTypedMessageListener), "alpha");

        super.setWebViewClient(tokenScriptClient);

        setWebChromeClient(new WebChromeClient()
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg)
            {
                if (!showingError && msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                {
                    String errorMessage = RENDERING_ERROR.replace("${ERR1}", msg.message());
                    showError(errorMessage);
                }
                return true;
            }
        });
    }

    public void showError(String error)
    {
        showingError = true;
        loadData(error, "text/html", "utf-8");
    }

    @Override
    public void setWebChromeClient(WebChromeClient client)
    {
        super.setWebChromeClient(client);
    }

    @JavascriptInterface
    public void onValue(String data)
    {
        System.out.println(data);
    }

    public void setWalletAddress(@NonNull Address address)
    {
        jsInjectorClient.setWalletAddress(address);
    }

    public void setupWindowCallback(@NonNull FunctionCallback callback)
    {
        setWebChromeClient(
                new WebChromeClient()
                {
                    @Override
                    public void onCloseWindow(WebView window)
                    {
                        callback.functionSuccess();
                    }
                }
        );
    }

    public void setChainId(int chainId) {
        jsInjectorClient.setChainId(chainId);
    }

    public void setRpcUrl(@NonNull int chainId) {
        jsInjectorClient.setRpcUrl(EthereumNetworkRepository.getNodeURLByNetworkId(chainId));
    }

    public void onSignPersonalMessageSuccessful(Message message, String signHex) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void setKeyboardListenerCallback(WebCompletionCallback cpCallback)
    {
        keyPressCallback = cpCallback;
    }

    @Override
    public String getUrl()
    {
        return "TokenScript";
    }

    public void callToJS(String function) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            post(() -> evaluateJavascript(function, value -> Log.d("WEB_VIEW", value)));
        }
    }

    @JavascriptInterface
    public void TScallToJS(String fName, String script, TokenScriptRenderCallback cb)
    {
        post(() -> evaluateJavascript(script, value -> cb.callToJSComplete(fName, value)));
    }

    @JavascriptInterface
    public void callbackToJS(long callbackId, String function, String param) {
        String callback = String.format(function, callbackId, param);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            post(() -> evaluateJavascript(callback, value -> Log.d("WEB_VIEW", value)));
        }
    }

    public void setOnSignPersonalMessageListener(@Nullable OnSignPersonalMessageListener onSignPersonalMessageListener) {
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
    }

    private final OnSignTransactionListener innerOnSignTransactionListener = new OnSignTransactionListener() {
        @Override
        public void onSignTransaction(Web3Transaction transaction, String url) {

        }
    };

    private final OnSignMessageListener innerOnSignMessageListener = new OnSignMessageListener() {
        @Override
        public void onSignMessage(Message message) {

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

        }
    };

    public void onSignCancel(Message message) {
        long callbackId = message.leafPosition;
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    public void setOnReadyCallback(PageReadyCallback holder)
    {
        assetHolder = holder;
    }

    public String injectWeb3TokenInit(Context ctx, String view, String tokenContent, BigInteger tokenId)
    {
        return jsInjectorClient.injectWeb3TokenInit(ctx, view, tokenContent, tokenId);
    }

    public String injectJS(String view, String buildToken)
    {
        return jsInjectorClient.injectJS(view, buildToken);
    }

    public String injectJSAtEnd(String view, String JSCode)
    {
        return jsInjectorClient.injectJSAtEnd(view, JSCode);
    }

    public String injectStyleAndWrapper(String viewData, String style)
    {
        return jsInjectorClient.injectStyleAndWrap(viewData, style);
    }

    public void setLayout(Token token, boolean iconified)
    {
        if (iconified && token.iconifiedWebviewHeight > 0)
        {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, token.iconifiedWebviewHeight);
            setLayoutParams(params);
        }
        else if (token.nonIconifiedWebviewHeight > 0)
        {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, token.nonIconifiedWebviewHeight);
            setLayoutParams(params);
        }
    }

    private class TokenScriptClient extends WebViewClient
    {
        private boolean loadingFinished = true;
        private boolean redirect = false;
        private Web3TokenView parent;

        public TokenScriptClient(Web3TokenView web3)
        {
            super();
            parent = web3;
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            if (assetHolder != null)
                assetHolder.onPageRendered(view);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url)
        {
            super.onPageCommitVisible(view, url);
            if (assetHolder != null)
                assetHolder.onPageLoaded(view);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
            {
                if (keyPressCallback != null) keyPressCallback.enterKeyPressed();
            }
            super.onUnhandledKeyEvent(view, event);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
        {
            showError(RENDERING_ERROR.replace("${ERR1}", error.getDescription()));
        }
    }
}
