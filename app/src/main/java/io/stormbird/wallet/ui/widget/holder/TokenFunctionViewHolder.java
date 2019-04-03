package io.stormbird.wallet.ui.widget.holder;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.web3.JsInjectorClient;
import io.stormbird.wallet.web3.Web3TokenView;
import io.stormbird.wallet.web3.entity.PageReadyCallback;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.ScriptFunction;

import java.math.BigInteger;

/**
 * Created by James on 3/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewHolder extends BinderViewHolder<String> implements View.OnClickListener, PageReadyCallback, ScriptFunction
{
    public static final int VIEW_TYPE = 1012;

    private final Web3TokenView tokenView;
    private final Token token;

    public TokenFunctionViewHolder(int resId, ViewGroup parent, Token t)
    {
        super(resId, parent);
        tokenView = findViewById(R.id.token_frame);
        tokenView.setVisibility(View.VISIBLE);
        token = t;
        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable String view, @NonNull Bundle addition)
    {
        try
        {
            tokenView.loadData(view, "text/html", "utf-8");
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
    public void onPageLoaded()
    {

    }

    @Override
    public void callFunction(String function)
    {
        tokenView.callToJS(function + "()");
    }
}

