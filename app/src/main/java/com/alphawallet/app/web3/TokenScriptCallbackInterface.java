package com.alphawallet.app.web3;

import androidx.annotation.NonNull;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 13/05/2020.
 */
public class TokenScriptCallbackInterface {

    private final WebView webView;
    @NonNull
    private final OnSignPersonalMessageListener onSignPersonalMessageListener;
    @NonNull
    private final OnSetValuesListener onSetValuesListener;

    public TokenScriptCallbackInterface(
            WebView webView,
            @NonNull OnSignPersonalMessageListener onSignPersonalMessageListener,
            @NonNull OnSetValuesListener onSetValuesListener) {
        this.webView = webView;
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
        this.onSetValuesListener = onSetValuesListener;
    }

    @JavascriptInterface
    public void signPersonalMessage(int callbackId, String data) {
        webView.post(() -> onSignPersonalMessageListener.onSignPersonalMessage(new EthereumMessage(data, getUrl(), callbackId, SignMessageType.SIGN_PERSONAL_MESSAGE)));
    }

    private String getUrl() {
        return webView == null ? "" : webView.getUrl();
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
