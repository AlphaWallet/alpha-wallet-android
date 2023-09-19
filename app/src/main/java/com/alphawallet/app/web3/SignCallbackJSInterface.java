package com.alphawallet.app.web3;

import static com.alphawallet.app.entity.tokenscript.TokenscriptFunction.ZERO_ADDRESS;

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

import timber.log.Timber;

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
    @NonNull
    private final OnWalletActionListener onWalletActionListener;

    public SignCallbackJSInterface(
            WebView webView,
            @NonNull OnSignTransactionListener onSignTransactionListener,
            @NonNull OnSignMessageListener onSignMessageListener,
            @NonNull OnSignPersonalMessageListener onSignPersonalMessageListener,
            @NonNull OnSignTypedMessageListener onSignTypedMessageListener,
            @NotNull OnEthCallListener onEthCallListener,
            @NonNull OnWalletAddEthereumChainObjectListener onWalletAddEthereumChainObjectListener,
            @NonNull OnWalletActionListener onWalletActionListener) {
        this.webView = webView;
        this.onSignTransactionListener = onSignTransactionListener;
        this.onSignMessageListener = onSignMessageListener;
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
        this.onSignTypedMessageListener = onSignTypedMessageListener;
        this.onEthCallListener = onEthCallListener;
        this.onWalletAddEthereumChainObjectListener = onWalletAddEthereumChainObjectListener;
        this.onWalletActionListener = onWalletActionListener;
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
        if (value == null || value.equals("undefined")) value = "0";
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
    public void requestAccounts(long callbackId) {
        webView.post(() -> onWalletActionListener.onRequestAccounts(callbackId) );
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
                Timber.e(e);
            }
        });
    }

    @JavascriptInterface
    public void ethCall(int callbackId, String recipient) {
        try
        {
            JSONObject json = new JSONObject(recipient);
            DefaultBlockParameter defaultBlockParameter;
            String to = json.has("to") ? json.getString("to") : ZERO_ADDRESS;
            String payload = json.has("data") ? json.getString("data") : "0x";
            String value = json.has("value") ? json.getString("value") : null;
            String gasLimit = json.has("gas") ? json.getString("gas") : null;
            defaultBlockParameter = DefaultBlockParameterName.LATEST; //TODO: Take block param from query if present

            Web3Call call = new Web3Call(
                new Address(to),
                defaultBlockParameter,
                payload,
                value,
                gasLimit,
                callbackId);

            webView.post(() -> onEthCallListener.onEthCall(call));
        }
        catch (Exception e)
        {
            //
        }
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
                webView.post(() -> onWalletAddEthereumChainObjectListener.onWalletAddEthereumChainObject(callbackId, chainObj));
            }
        }
        catch (JsonSyntaxException e)
        {
            Timber.e(e);
        }
    }

    @JavascriptInterface
    public void walletSwitchEthereumChain(int callbackId, String msgParams) {
        try
        { //{"chainId":"0x89","chainType":"ETH"}
            WalletAddEthereumChainObject chainObj = new Gson().fromJson(msgParams, WalletAddEthereumChainObject.class);
            if (!TextUtils.isEmpty(chainObj.chainId))
            {
                webView.post(() -> onWalletActionListener.onWalletSwitchEthereumChain(callbackId, chainObj));// onWalletAddEthereumChainObjectListener.onWalletAddEthereumChainObject(chainObj));
            }
        }
        catch (JsonSyntaxException e)
        {
            Timber.e(e);
        }
    }

    private String getUrl() {
        return webView == null ? "" : webView.getUrl();
    }

    private String getDomainName() {
        return webView == null ? "" : Utils.getDomainName(webView.getUrl());
    }
}
