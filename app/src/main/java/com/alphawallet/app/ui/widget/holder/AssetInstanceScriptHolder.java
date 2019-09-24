package com.alphawallet.app.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.PageReadyCallback;

import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.TokenFunctionActivity;
import com.alphawallet.app.ui.widget.OnTokenClickListener;

import static com.alphawallet.app.C.Key.TICKET;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<TicketRange> implements PageReadyCallback, Runnable
{
    public static final int VIEW_TYPE = 1011;

    private final Web3TokenView tokenView;
    private final Token token;
    private final RelativeLayout frameLayout;
    private final LinearLayout webWrapper;
    private final boolean iconified;
    private OnTokenClickListener tokenClickListener;
    private final AppCompatRadioButton itemSelect;
    private final AssetDefinitionService assetDefinitionService; //need to cache this locally, unless we cache every string we need in the constructor
    private boolean activeClick;
    private final Handler handler;

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService, boolean iconified)
    {
        super(resId, parent);
        frameLayout = findViewById(R.id.layout_select_ticket);
        tokenView = findViewById(R.id.web3_tokenview);
        assetDefinitionService = assetService;
        webWrapper = findViewById(R.id.click_layer);
        itemSelect = findViewById(R.id.radioBox);
        token = t;
        tokenView.setOnReadyCallback(this);
        this.iconified = iconified;
        handler = new Handler();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        activeClick = false;
        try
        {
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
            token.displayTicketHolder(data, frameLayout, assetDefinitionService, getContext(), iconified);

            if (iconified)
            {
                webWrapper.setVisibility((View.VISIBLE));
                webWrapper.setOnClickListener(v -> handleClick(v, data));
                webWrapper.setOnLongClickListener(v -> handleLongClick(v, data));
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
    public void onPageLoaded()
    {
        tokenView.callToJS("refresh()");
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
            intent.putExtra(TICKET, token);
            intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(data.tokenIds, false));
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

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener)
    {
        tokenClickListener = onTokenClickListener;
    }

    @Override
    public void run()
    {
        activeClick = false;
    }
}
