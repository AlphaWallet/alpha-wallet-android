package io.stormbird.wallet.web3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import io.stormbird.token.entity.MagicLinkInfo;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.PageReadyCallback;

/**
 * Created by James on 3/04/2019.
 * Stormbird in Singapore
 */
public class Web3TokenView extends WebView
{
    private static final String JS_FUNCTION_CALL = "%1$s(%2$s)";

    private JsInjectorClient jsInjectorClient;
    private TokenScriptClient tokenScriptClient;
    private PageReadyCallback assetHolder;

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
        tokenScriptClient = new TokenScriptClient();
        jsInjectorClient = new JsInjectorClient(getContext());
        WebSettings webSettings = super.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        setInitialScale(0);
        addJavascriptInterface(this, "listener");

        super.setWebViewClient(tokenScriptClient);
    }

    @JavascriptInterface
    public void onValue(String data)
    {
        System.out.println(data);
    }

    public void setWalletAddress(@NonNull Address address) {
        jsInjectorClient.setWalletAddress(address);
    }

    public void setChainId(int chainId) {
        jsInjectorClient.setChainId(chainId);
    }

    public void setRpcUrl(@NonNull int chainId) {
        jsInjectorClient.setRpcUrl(MagicLinkInfo.getNodeURLByNetworkId(chainId));
    }

    public void callToJS(String function) {
        //String callback = String.format(function, callbackId, param);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            post(() -> evaluateJavascript(function, value -> Log.d("WEB_VIEW", value)));
        }
    }

    public void callToJS(String function, String args) {
        String callback = String.format(JS_FUNCTION_CALL, function, args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            post(() -> evaluateJavascript(callback, value -> Log.d("WEB_VIEW", value)));
        }
    }

    public void setOnReadyCallback(PageReadyCallback holder)
    {
        assetHolder = holder;
    }

    private class TokenScriptClient extends WebViewClient
    {
        private boolean loadingFinished = true;
        private boolean redirect = false;

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            if (assetHolder != null)
                assetHolder.onPageLoaded();
        }
    }
}
