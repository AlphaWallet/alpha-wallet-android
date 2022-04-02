package com.alphawallet.app.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.TokenFunctionActivity;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.token.entity.TicketRange;
import com.google.android.material.radiobutton.MaterialRadioButton;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<TicketRange> implements PageReadyCallback, Runnable
{
    public static final int VIEW_TYPE = 1011;

    private final Web3TokenView tokenView;
    private final Token token;
    private final LinearLayout clickWrapper;
    private final LinearLayout webWrapper;
    private final boolean iconified;
    private TokensAdapterCallback tokenClickListener;
    private final MaterialRadioButton itemSelect;
    private final AssetDefinitionService assetDefinitionService; //need to cache this locally, unless we cache every string we need in the constructor
    private boolean activeClick;
    private final Handler handler = new Handler();

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService, boolean iconified)
    {
        super(resId, parent);
        tokenView = findViewById(R.id.web3_tokenview);
        webWrapper = findViewById(R.id.layout_webwrapper);
        assetDefinitionService = assetService;
        clickWrapper = findViewById(R.id.click_layer);
        itemSelect = findViewById(R.id.radioBox);
        token = t;
        tokenView.setOnReadyCallback(this);
        this.iconified = iconified;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        activeClick = false;
        try
        {
            tokenView.setLayout(token, iconified);
            if (data.tokenIds.size() == 0) { fillEmpty(); return; }
            if (data.exposeRadio)
            {
                itemSelect.setVisibility(View.VISIBLE);
            }
            else
            {
                itemSelect.setVisibility(View.GONE);
            }

            itemSelect.setChecked(data.isChecked);
            tokenView.displayTicketHolder(token, data, assetDefinitionService, iconified);
            tokenView.setOnReadyCallback(this);

            if (iconified)
            {
                clickWrapper.setVisibility((View.VISIBLE));
                clickWrapper.setOnClickListener(v -> handleClick(v, data));
                clickWrapper.setOnLongClickListener(v -> handleLongClick(v, data));
            }
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
    public void onPageLoaded(WebView view)
    {
        tokenView.callToJS("refresh()");
    }

    @Override
    public void onPageRendered(WebView view)
    {
        webWrapper.setVisibility(View.VISIBLE);
    }

    public void handleClick(View v, TicketRange data)
    {
        if (data.exposeRadio)
        {
            if (!data.isChecked)
            {
                tokenClickListener.onTokenClick(v,token,data.tokenIds, true);
                data.isChecked = true;
                itemSelect.setChecked(true);
            }
        }
        else
        {
            if (activeClick) return;
            activeClick = true;
            handler.postDelayed(this, 500);
            Intent intent = new Intent(getContext(), TokenFunctionActivity.class);
            intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
            intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
            intent.putExtra(C.EXTRA_TOKEN_ID, Utils.bigIntListToString(data.tokenIds, false));
            intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            getContext().startActivity(intent);
        }
    }

    private boolean handleLongClick(View v, TicketRange data)
    {
        //open up the radio view and signal to holding app
        tokenClickListener.onLongTokenClick(v, token, data.tokenIds);
        data.isChecked = true;
        itemSelect.setChecked(true);
        return true;
    }

    public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback)
    {
        tokenClickListener = tokensAdapterCallback;
    }

    @Override
    public void run()
    {
        activeClick = false;
    }
}
