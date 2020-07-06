package com.alphawallet.app.ui.widget.holder;

import android.content.Intent;
import android.graphics.drawable.Drawable;
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

import com.alphawallet.token.entity.TicketRange;
import com.bumptech.glide.Glide;
import com.alphawallet.app.util.KittyUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.TokenDetailActivity;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaHolder extends BinderViewHolder<TicketRange> implements Runnable {
    public static final int VIEW_TYPE = 1302;
    protected final Token token;
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
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        String assetName;
        activeClick = false;
        handler = new Handler();
        //retrieve asset from token
        Asset asset = getAsset(data);

        if (asset.getName() != null && !asset.getName().equals("null")) {
            assetName = asset.getName();
        } else {
            assetName = "ID# " + String.valueOf(asset.getTokenId());
        }
        titleText.setText(assetName);

        if (data.exposeRadio)
        {
            asset.exposeRadio = true;
            itemSelect.setVisibility(View.VISIBLE);
            itemSelect.setChecked(data.isChecked);
        }
        else
        {
            asset.exposeRadio = false;
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
                .listener(requestListener)
                .into(image);

        layoutToken.setOnClickListener(v -> handleClick(v, data));
        layoutToken.setOnLongClickListener(v -> handleLongClick(v, data));
    }

    /**
     * Prevent glide dumping log errors - it is expected that load will fail
     */
    private RequestListener<Drawable> requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

    private Asset getAsset(TicketRange data)
    {
        BigInteger tokenId = data.tokenIds.get(0); //range is never grouped for ERC721 tickets
        return token.getAssetForToken(tokenId.toString());
    }

    public void handleClick(View v, TicketRange data)
    {
        if (data.exposeRadio)
        {
            if (!data.isChecked)
            {
                tokenClickListener.onTokenClick(v, token, data.tokenIds, true);
                data.isChecked = true;
                itemSelect.setChecked(true);
            }
            else
            {
                tokenClickListener.onTokenClick(v, token, data.tokenIds, false);
                data.isChecked = false;
                itemSelect.setChecked(false);
            }
        }
        else
        {
            if (activeClick) return;
            activeClick = true;
            handler.postDelayed(this, 500);
            Intent intent = new Intent(getContext(), TokenDetailActivity.class);
            intent.putExtra("asset", getAsset(data));
            intent.putExtra("token", token);
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
