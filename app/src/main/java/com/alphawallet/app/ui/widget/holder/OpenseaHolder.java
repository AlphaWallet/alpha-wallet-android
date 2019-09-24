package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.alphawallet.app.util.KittyUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.ui.TokenDetailActivity;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.ui.widget.OnTokenClickListener;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaHolder extends BinderViewHolder<Asset> implements Runnable {
    public static final int VIEW_TYPE = 1302;
    private final Token token;
    private final TextView titleText;
    private final ImageView image;
    private final TextView generation;
    private final TextView cooldown;
    private final TextView statusText;
    private final LinearLayout layoutToken;
    private OnTokenClickListener tokenClickListener;
    private final AppCompatRadioButton itemSelect;
    private Handler handler;
    private boolean activeClick;

    public OpenseaHolder(int resId, ViewGroup parent, Token token) {
        super(resId, parent);
        titleText = findViewById(R.id.name);
        image = findViewById(R.id.image_view);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        statusText = findViewById(R.id.status);
        layoutToken = findViewById(R.id.layout_token);
        itemSelect = findViewById(R.id.radioBox);
        this.token = token;
    }

    @Override
    public void bind(@Nullable Asset asset, @NonNull Bundle addition) {
        String assetName;
        activeClick = false;
        handler = new Handler();
        if (asset.getName() != null && !asset.getName().equals("null")) {
            assetName = asset.getName();
        } else {
            assetName = "ID# " + String.valueOf(asset.getTokenId());
        }
        titleText.setText(assetName);

        if (asset.exposeRadio)
        {
            itemSelect.setVisibility(View.VISIBLE);
            itemSelect.setChecked(asset.isChecked);
        }
        else
        {
            itemSelect.setVisibility(View.GONE);
        }

        if (asset.getTraitFromType("generation") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("generation").getValue()));
        } else if (asset.getTraitFromType("gen") != null){
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("gen").getValue()));
        } else {
            generation.setVisibility(View.GONE);
        }

        if (asset.getTraitFromType("cooldown_index") != null) {
            cooldown.setText(String.format("%s Cooldown",
                    KittyUtils.parseCooldownIndex(
                            asset.getTraitFromType("cooldown_index").getValue())));
        } else if (asset.getTraitFromType("cooldown") != null) { // Non-CK
            cooldown.setText(String.format("%s Cooldown",
                    asset.getTraitFromType("cooldown").getValue()));
        } else {
            cooldown.setVisibility(View.GONE);
        }

        Glide.with(getContext())
                .load(asset.getImagePreviewUrl())
                .into(image);

        layoutToken.setOnClickListener(v -> handleClick(v, asset));
        layoutToken.setOnLongClickListener(v -> handleLongClick(v, asset));
    }

    private void setStatus(C.TokenStatus status) {
        if (status == C.TokenStatus.PENDING) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setBackgroundResource(R.drawable.background_status_pending);
            statusText.setText(R.string.status_pending);
        } else if (status == C.TokenStatus.INCOMPLETE) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setBackgroundResource(R.drawable.background_status_incomplete);
            statusText.setText(R.string.status_incomplete_data);
        } else {
            statusText.setVisibility(View.GONE);
        }
    }

    public void handleClick(View v, Asset asset)
    {
        if (asset.exposeRadio)
        {
            if (!asset.isChecked)
            {
                tokenClickListener.onTokenClick(v, token, new ArrayList<>(Arrays.asList(new BigInteger(asset.getTokenId()))), true);
                asset.isChecked = true;
                itemSelect.setChecked(true);
            }
            else
            {
                tokenClickListener.onTokenClick(v, token, new ArrayList<>(Arrays.asList(new BigInteger(asset.getTokenId()))), false);
                asset.isChecked = false;
                itemSelect.setChecked(false);
            }
        }
        else
        {
            if (activeClick) return;
            activeClick = true;
            handler.postDelayed(this, 500);
            Intent intent = new Intent(getContext(), TokenDetailActivity.class);
            intent.putExtra("asset", asset);
            intent.putExtra("token", token);
            getContext().startActivity(intent);
        }
    }

    private boolean handleLongClick(View v, Asset asset)
    {
        //open up the radio view and signal to holding app
        tokenClickListener.onLongTokenClick(v, token, new ArrayList<>(Arrays.asList(new BigInteger(asset.getTokenId()))));
        asset.isChecked = true;
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
