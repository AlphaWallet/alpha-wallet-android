package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import android.widget.RelativeLayout;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.ui.widget.OnOpenseaAssetCheckListener;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;

/**
 * Created by James on 12/11/2018.
 * Stormbird in Singapore
 */
public class OpenseaSelectHolder extends OpenseaHolder
{
    private final AppCompatRadioButton select;
    private OnOpenseaAssetCheckListener onTokenCheckListener;
    private final RelativeLayout ticketLayout;

    public OpenseaSelectHolder(int resId, ViewGroup parent, Token token)
    {
        super(resId, parent, token, null, true);
        ticketLayout = findViewById(R.id.layout_select_ticket);
        select = findViewById(R.id.radioBox);
    }

//    @Override
//    public void bind(@Nullable Asset asset, @NonNull Bundle addition)
//    {
//        super.bind(asset, addition);
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        BigInteger tokenId = data.tokenIds.get(0); //range is never grouped for ERC721 tickets
        super.bind(data, addition);
        Asset      asset   = token.getAssetForToken(tokenId.toString());
        select.setVisibility(View.VISIBLE);

        select.setOnCheckedChangeListener(null); //have to invalidate listener first otherwise we trigger cached listener and create infinite loop
        select.setChecked(asset.isChecked);

        select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if (b)
                {
                    onTokenCheckListener.onAssetCheck(asset);
                }
            }
        });

        ticketLayout.setOnClickListener(v -> {
            select.setChecked(true);
        });
    }

    public void setOnTokenCheckListener(OnOpenseaAssetCheckListener onTokenCheckListener)
    {
        this.onTokenCheckListener = onTokenCheckListener;
    }
}
