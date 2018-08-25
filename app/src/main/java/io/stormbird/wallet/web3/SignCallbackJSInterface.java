package io.stormbird.wallet.web3;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;

import java.math.BigInteger;

import io.stormbird.wallet.util.Hex;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.Message;
import io.stormbird.wallet.web3.entity.Transaction;
import io.stormbird.wallet.web3.entity.TypedData;

public class SignCallbackJSInterface {

    private final WebView webView;
    @NonNull
    private final OnSignTransactionListener onSignTransactionListener;
    @NonNull
    private final OnSignMessageListener onSignMessageListener;
    @NonNull
    private final OnSignPersonalMessageListener onSignPersonalMessageListener;
    @NonNull
    private final OnSignTypedMessageListener onSignTypedMessageListener;
    @NonNull
    private final OnVerifyListener onVerifyListener;

    public SignCallbackJSInterface(
            WebView webView,
            @NonNull OnSignTransactionListener onSignTransactionListener,
            @NonNull OnSignMessageListener onSignMessageListener,
            @NonNull OnSignPersonalMessageListener onSignPersonalMessageListener,
            @NonNull OnSignTypedMessageListener onSignTypedMessageListener,
            @NonNull OnVerifyListener onVerifyListener) {
        this.webView = webView;
        this.onSignTransactionListener = onSignTransactionListener;
        this.onSignMessageListener = onSignMessageListener;
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
        this.onSignTypedMessageListener = onSignTypedMessageListener;
        this.onVerifyListener = onVerifyListener;
    }

    @JavascriptInterface
    public void signTransaction(
            int callbackId,
            String recipient,
            String value,
            String nonce,
            String gasLimit,
            String gasPrice,
            String payload) {
        Transaction transaction = new Transaction(
                TextUtils.isEmpty(recipient) ? Address.EMPTY : new Address(recipient),
                null,
                Hex.hexToBigInteger(value),
                Hex.hexToBigInteger(gasPrice, BigInteger.ZERO),
                Hex.hexToLong(gasLimit, 0),
                Hex.hexToLong(nonce, -1),
                payload,
                callbackId);
        onSignTransactionListener.onSignTransaction(transaction);

    }

    @JavascriptInterface
    public void signMessage(int callbackId, String data) {
        webView.post(() -> onSignMessageListener.onSignMessage(new Message<>(data, getUrl(), callbackId)));
    }

    @JavascriptInterface
    public void signPersonalMessage(int callbackId, String data) {
        webView.post(() -> onSignPersonalMessageListener.onSignPersonalMessage(new Message<>(data, getUrl(), callbackId)));
    }

    @JavascriptInterface
    public void signTypedMessage(int callbackId, String data) {
        webView.post(() -> {
            TrustProviderTypedData[] rawData = new Gson().fromJson(data, TrustProviderTypedData[].class);
            int len = rawData.length;
            TypedData[] typedData = new TypedData[len];
            for (int i = 0; i < len; i++) {
                typedData[i] = new TypedData(rawData[i].name, rawData[i].type, rawData[i].value);
            }
            onSignTypedMessageListener.onSignTypedMessage(new Message<>(typedData, getUrl(), callbackId));
        });
    }

    @JavascriptInterface
    public void verify(String message, String signHex) {
        onVerifyListener.onVerify(message, signHex);
    }

    private String getUrl() {
        return webView == null ? "" : webView.getUrl();
    }

    private static class TrustProviderTypedData {
        public String name;
        public String type;
        public Object value;
    }
}
