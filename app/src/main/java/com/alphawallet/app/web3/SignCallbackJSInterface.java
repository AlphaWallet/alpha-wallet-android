package com.alphawallet.app.web3;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.ProviderTypedData;
import com.alphawallet.token.entity.SignMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import wallet.core.jni.Hash;

public class SignCallbackJSInterface
{
    private final WebView webView;
    @NonNull
    private final OnSignTransactionListener onSignTransactionListener;
    @NonNull
    private final OnSignMessageListener onSignMessageListener;
    @NonNull
    private final OnSignPersonalMessageListener onSignPersonalMessageListener;
    @NonNull
    private final OnSignTypedMessageListener onSignTypedMessageListener;

    public SignCallbackJSInterface(
            WebView webView,
            @NonNull OnSignTransactionListener onSignTransactionListener,
            @NonNull OnSignMessageListener onSignMessageListener,
            @NonNull OnSignPersonalMessageListener onSignPersonalMessageListener,
            @NonNull OnSignTypedMessageListener onSignTypedMessageListener) {
        this.webView = webView;
        this.onSignTransactionListener = onSignTransactionListener;
        this.onSignMessageListener = onSignMessageListener;
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
        this.onSignTypedMessageListener = onSignTypedMessageListener;
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
        if (value.equals("undefined")) value = "0";
        if (gasPrice == null) gasPrice = "0";
        Web3Transaction transaction = new Web3Transaction(
                TextUtils.isEmpty(recipient) ? Address.EMPTY : new Address(recipient),
                null,
                Hex.hexToBigInteger(value),
                Hex.hexToBigInteger(gasPrice, BigInteger.ZERO),
                Hex.hexToBigInteger(gasLimit, BigInteger.ZERO),
                Hex.hexToLong(nonce, -1),
                payload,
                callbackId);

        webView.post(() -> onSignTransactionListener.onSignTransaction(transaction, getUrl()));
    }

    @JavascriptInterface
    public void signMessage(int callbackId, String data) {
        webView.post(() -> onSignMessageListener.onSignMessage(new EthereumMessage(data, getUrl(), callbackId, SignMessageType.SIGN_MESSAGE)));
    }

    @JavascriptInterface
    public void signPersonalMessage(int callbackId, String data) {
        webView.post(() -> onSignPersonalMessageListener.onSignPersonalMessage(new EthereumMessage(data, getUrl(), callbackId, SignMessageType.SIGN_PERSONAL_MESSAGE)));
    }

    @JavascriptInterface
    public void signTypedMessage(int callbackId, String data) {
        webView.post(() -> {
            try
            {
                JSONObject obj = new JSONObject(data);
                String address = obj.getString("from");
                String messageData = obj.getString("data");
                CryptoFunctions cryptoFunctions = new CryptoFunctions();

                EthereumTypedMessage message = new EthereumTypedMessage(messageData, getDomainName(), callbackId, cryptoFunctions);
                onSignTypedMessageListener.onSignTypedMessage(message);
            }
            catch (Exception e)
            {
                EthereumTypedMessage message = new EthereumTypedMessage(null, "", getDomainName(), callbackId);
                onSignTypedMessageListener.onSignTypedMessage(message);
                e.printStackTrace();
            }
        });
    }

    private String getUrl() {
        return webView == null ? "" : webView.getUrl();
    }

    private String getDomainName() {
        return webView == null ? "" : Utils.getDomainName(webView.getUrl());
    }
}
