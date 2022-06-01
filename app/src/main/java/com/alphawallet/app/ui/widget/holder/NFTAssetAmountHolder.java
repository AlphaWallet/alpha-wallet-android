package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

/**
 * Created by JB on 22/08/2021.
 */
public class NFTAssetAmountHolder extends BinderViewHolder<NFTAsset>
{
    public static final int VIEW_TYPE = 1304;

    private final ImageView icon;
    private final TextView assetCount;

    public NFTAssetAmountHolder(ViewGroup parent)
    {
        super(R.layout.item_nftasset_grid, parent);

        icon = findViewById(R.id.icon);
        assetCount = findViewById(R.id.count);
    }

    @Override
    public void bind(@Nullable NFTAsset asset, @NonNull Bundle addition)
    {
        Glide.with(getContext())
                .load(asset.getThumbnail())
                .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                .into(new DrawableImageViewTarget(icon));

        assetCount.setText(getString(R.string.asset_count_val, asset.getSelectedBalance().toString()));
    }
}
