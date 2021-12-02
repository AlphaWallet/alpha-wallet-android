package com.alphawallet.app.web3;

import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.CryptoFunctions;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Call;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigInteger;

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
    @NonNull
    private final OnEthCallListener onEthCallListener;
    @NonNull
    private final OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener;

    public SignCallbackJSInterface(
            WebView webView,
            @NonNull OnSignTransactionListener onSignTransactionListener,
            @NonNull OnSignMessageListener onSignMessageListener,
            @NonNull OnSignPersonalMessageListener onSignPersonalMessageListener,
            @NonNull OnSignTypedMessageListener onSignTypedMessageListener,
            @NotNull OnEthCallListener onEthCallListener,
            @NonNull OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener) {
        this.webView = webView;
        this.onSignTransactionListener = onSignTransactionListener;
        this.onSignMessageListener = onSignMessageListener;
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
        this.onSignTypedMessageListener = onSignTypedMessageListener;
        this.onEthCallListener = onEthCallListener;
        this.onWalletAddEthereumChainObjectListener = onWalletAddEthereumChainObjectListener;
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
        if (value.equals("undefined") || value == null) value = "0";
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
                if (BuildConfig.DEBUG) e.printStackTrace();
            }
        });
    }

    @JavascriptInterface
    public void ethCall(int callbackId, String recipient, String payload) {
        DefaultBlockParameter defaultBlockParameter;
        if (payload.equals("undefined")) payload = "0x";
        defaultBlockParameter = DefaultBlockParameterName.LATEST;

        Web3Call call = new Web3Call(
                new Address(recipient),
                defaultBlockParameter,
                payload,
                callbackId);

        webView.post(() -> onEthCallListener.onEthCall(call));
    }

    @JavascriptInterface
    public void walletAddEthereumChain(int callbackId, String msgParams) {
        //TODO: Implement custom chains from dapp browser: see OnWalletAddEthereumChainObject in class DappBrowserFragment
        //First draft: attempt to match this chain with known chains; switch to known chain if we match
        try
        {
            WalletAddEthereumChainObject chainObj = new Gson().fromJson(msgParams, WalletAddEthereumChainObject.class);
            if (!TextUtils.isEmpty(chainObj.chainId))
            {
                webView.post(() -> onWalletAddEthereumChainObjectListener.onWalletAddEthereumChainObject(chainObj));
            }
        }
        catch (JsonSyntaxException e)
        {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
    }

    private String getUrl() {
        return webView == null ? "" : webView.getUrl();
    }

    private String getDomainName() {
        return webView == null ? "" : Utils.getDomainName(webView.getUrl());
    }
}
