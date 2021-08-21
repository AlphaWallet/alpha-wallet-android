package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.jetbrains.annotations.NotNull;

/**
 * Created by JB on 19/08/2021.
 */
public class NFTAssetHolder extends BinderViewHolder<NFTAsset>
{
    public static final int VIEW_TYPE = 1303;

    final ImageView icon;
    final TextView title;
    final TextView assetCategory;
    final TextView assetCount;
    final TextView selectionAmount;

    public NFTAssetHolder(ViewGroup parent)
    {
        super(R.layout.item_erc1155_asset_select, parent);

        icon = findViewById(R.id.icon);
        title = findViewById(R.id.title);
        assetCategory = findViewById(R.id.subtitle);
        assetCount = findViewById(R.id.count);
        selectionAmount = findViewById(R.id.text_count);

        findViewById(R.id.checkbox).setVisibility(View.GONE);
    }

    @Override
    public void bind(@Nullable NFTAsset asset, @NonNull Bundle addition)
    {
        title.setText(asset.getName());
        assetCategory.setText("Placeholder: Asset type");
        Glide.with(getContext())
                .load(asset.getThumbnail())
                .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                .into(icon);

        if (asset.isAssetMultiple())
        {
            assetCount.setVisibility(View.VISIBLE);
            assetCount.setText(getString(R.string.asset_count_val, asset.getSelectedBalance().toString()));
        }
    }
}
