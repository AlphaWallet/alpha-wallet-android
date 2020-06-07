package com.alphawallet.app.web3;

<<<<<<< HEAD
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
=======
import androidx.annotation.NonNull;
>>>>>>> e3074436a... Attempt to upgrade to AndroidX
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.MessageUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.ProviderTypedData;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

import wallet.core.jni.Hash;

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
        webView.post(() -> onSignMessageListener.onSignMessage(new EthereumMessage(data, getUrl(), callbackId)));
    }

    @JavascriptInterface
    public void signPersonalMessage(int callbackId, String data) {
        webView.post(() -> onSignPersonalMessageListener.onSignPersonalMessage(new EthereumMessage(data, getUrl(), callbackId, true)));
    }

    @JavascriptInterface
    public void signTypedMessage(int callbackId, String data) {
        webView.post(() -> {
            try
            {
                JSONObject obj = new JSONObject(data);
                String address = obj.getString("from");
                String messageData = obj.getString("data");

                //TODO: Find a common place for this code (duplicated in WalletConnectActivity)
                //TODO: - if moved to EthereumTypedMessage then we need to add Web3j helper classes to this library
                //TODO: use a more deterministic method to detect EIP712 vs Legacy SignTypedData -
                //TODO: Currently we throw if the data can't be decoded as Legacy and assume it must be EIP712
                try
                {
                    ProviderTypedData[] rawData = new Gson().fromJson(messageData, ProviderTypedData[].class);
                    ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream();
                    writeBuffer.write(Hash.keccak256(MessageUtils.encodeParams(rawData)));
                    writeBuffer.write(Hash.keccak256(MessageUtils.encodeValues(rawData)));
                    CharSequence message = MessageUtils.formatTypedMessage(rawData);
                    onSignTypedMessageListener.onSignTypedMessage(new EthereumTypedMessage(writeBuffer.toByteArray(), message, getDomainName(), callbackId));
                }
                catch (JsonSyntaxException e)
                {
                    StructuredDataEncoder eip721Object = new StructuredDataEncoder(messageData);
                    CharSequence message = MessageUtils.formatEIP721Message(eip721Object);
                    onSignTypedMessageListener.onSignTypedMessage(new EthereumTypedMessage(eip721Object.getStructuredData(), message, getDomainName(), callbackId));
                }
            }
            catch (Exception e)
            {
                onSignTypedMessageListener.onSignTypedMessage(new EthereumTypedMessage(null, "", getUrl(), callbackId));
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
