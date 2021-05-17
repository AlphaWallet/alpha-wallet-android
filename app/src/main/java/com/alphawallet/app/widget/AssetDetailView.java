package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.bumptech.glide.Glide;

/**
 * Created by Jenny Jingjing Li on 13/05/2021
 */

public class AssetDetailView extends LinearLayout
{
    private final TextView assetName;
    private final ImageView assetImage;
    private final TextView assetDescription;
    private final LinearLayout layoutDetails;
    private final ImageView assetDetails;
    private final LinearLayout layoutHolder;

    public AssetDetailView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_asset_detail, this);
        assetName = findViewById(R.id.text_asset_name);
        assetImage = findViewById(R.id.image_asset);
        assetDescription = findViewById(R.id.text_asset_description);
        assetDetails = findViewById(R.id.image_more);
        layoutDetails = findViewById(R.id.layout_details);
        layoutHolder = findViewById(R.id.layout_holder);
    }

    public void setupAssetDetail(Token token, String tokenId)
    {
        Asset asset = token.getAssetForToken(tokenId);
        assetName.setText(asset.getName());

        Glide.with(this)
                .load(asset.getImagePreviewUrl())
                .into(assetImage);

        assetDescription.setText(asset.getDescription());

        layoutHolder.setOnClickListener(v -> {
            if (layoutDetails.getVisibility() == View.GONE)
            {
                layoutDetails.setVisibility(View.VISIBLE);
                assetDetails.setImageResource(R.drawable.ic_expand_less_black);
            }
            else
            {
                layoutDetails.setVisibility(View.GONE);
                assetDetails.setImageResource(R.drawable.ic_expand_more);
            }
        });

    }

}
