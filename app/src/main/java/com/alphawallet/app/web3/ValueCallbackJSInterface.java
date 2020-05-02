package com.alphawallet.app.web3;

import android.support.annotation.NonNull;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.alphawallet.app.web3.entity.Message;

/**
 * Created by JB on 1/05/2020.
 */
public class ValueCallbackJSInterface
{
    private final WebView webView;
    @NonNull
    private final OnSetValuesListener onSetValuesListener;

    public ValueCallbackJSInterface(
            WebView webView,
            @NonNull OnSetValuesListener onSetValuesListener)
    {
        this.webView = webView;
        this.onSetValuesListener = onSetValuesListener;
    }

    @JavascriptInterface
    public void setValues(String ref) {
        onSetValuesListener.setValues(ref);
    }
}
