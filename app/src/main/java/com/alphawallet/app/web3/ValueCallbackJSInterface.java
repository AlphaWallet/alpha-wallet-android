package com.alphawallet.app.web3;

import androidx.annotation.NonNull;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

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
    public void setValues(String jsonValuesFromTokenView) {
        Map<String, String> updates;
        try
        {
            updates = new Gson().fromJson(jsonValuesFromTokenView, new TypeToken<HashMap<String, String>>() {}.getType());
        }
        catch (Exception e)
        {
            updates = new HashMap<>();
        }

        onSetValuesListener.setValues(updates);
    }
}
