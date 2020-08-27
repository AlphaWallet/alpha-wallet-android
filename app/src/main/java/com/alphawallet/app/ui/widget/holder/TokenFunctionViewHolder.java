package com.alphawallet.app.ui.widget.holder;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.FunctionCallback;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.web3.entity.ScriptFunction;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.widget.SignMessageDialog;

import static com.alphawallet.app.ui.DappBrowserFragment.PERSONAL_MESSAGE_PREFIX;

/**
 * Created by James on 3/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewHolder extends BinderViewHolder<String> implements View.OnClickListener, PageReadyCallback, ScriptFunction, OnSignPersonalMessageListener
{
    public static final int VIEW_TYPE = 1012;

    private final Web3TokenView tokenView;
    private final Token token;
    private SignMessageDialog dialog;
    private final FunctionCallback functionCallback;
    private final AssetDefinitionService assetDefinitionService;
    private boolean reloaded;

    public TokenFunctionViewHolder(int resId, ViewGroup parent, Token t, FunctionCallback callback, AssetDefinitionService service)
    {
        super(resId, parent);
        tokenView = findViewById(R.id.token_frame);
        tokenView.setVisibility(View.VISIBLE);
        token = t;
        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
        tokenView.setOnSignPersonalMessageListener(this);
        tokenView.setupWindowCallback(callback);
        functionCallback = callback;
        assetDefinitionService = service;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable String view, @NonNull Bundle addition)
    {
        try
        {
            reloaded = false;
            String injectedView = tokenView.injectWeb3TokenInit(view, "", BigInteger.ONE);
            String style = assetDefinitionService.getTokenView(token.tokenInfo.chainId, token.getAddress(), "style");
            injectedView = tokenView.injectStyleAndWrapper(injectedView, style);

            String base64 = Base64.encodeToString(injectedView.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            tokenView.loadData(base64, "text/html; charset=utf-8", "base64");
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private void fillEmpty()
    {
        tokenView.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPageLoaded(WebView view)
    {
        tokenView.callToJS("refresh()");
    }

    @Override
    public void onPageRendered(WebView view)
    {
        if (!reloaded) tokenView.reload();
        reloaded = true;
    }

    @Override
    public void callFunction(String function, String arg)
    {
        tokenView.callToJS(function + "('" + arg + "')");
    }

    @Override
    public void onSignPersonalMessage(EthereumMessage message)
    {
        DAppFunction dAppFunction = new DAppFunction() {
            @Override
            public void DAppError(Throwable error, Signable message) {
                tokenView.onSignCancel(message);
                dialog.dismiss();
                functionCallback.functionFailed();
            }

            @Override
            public void DAppReturn(byte[] data, Signable message) {
                String signHex = Numeric.toHexString(data);
                signHex = Numeric.cleanHexPrefix(signHex);
                tokenView.onSignPersonalMessageSuccessful(message, signHex);
                dialog.dismiss();
            }
        };

        dialog = new SignMessageDialog(getContext(), message);
        dialog.setAddress(token.getAddress());
        dialog.setMessage(message.getMessage());
        dialog.setOnApproveListener(v -> {
            functionCallback.signMessage(message, dAppFunction);
        });
        dialog.setOnRejectListener(v -> {
            tokenView.onSignCancel(message);
            dialog.dismiss();
        });
        dialog.show();
    }
}

