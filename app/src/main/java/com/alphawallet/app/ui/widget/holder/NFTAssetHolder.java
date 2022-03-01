package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.widget.NFTImageView;

import java.math.BigInteger;

/**
 * Created by JB on 19/08/2021.
 */
public class NFTAssetHolder extends BinderViewHolder<Pair<BigInteger, NFTAsset>>
{
    public static final int VIEW_TYPE = 1303;

    final NFTImageView icon;
    final TextView title;
    final TextView assetCategory;
    final TextView assetCount;
    final TextView selectionAmount;
    final TextView tokenId;

    public NFTAssetHolder(ViewGroup parent)
    {
        super(R.layout.item_erc1155_asset_select, parent);

        icon = findViewById(R.id.icon);
        title = findViewById(R.id.title);
        assetCategory = findViewById(R.id.subtitle);
        assetCount = findViewById(R.id.count);
        selectionAmount = findViewById(R.id.text_count);
        tokenId = findViewById(R.id.token_id);
    }

    @Override
    public void bind(@Nullable Pair<BigInteger, NFTAsset> asset, @NonNull Bundle addition)
    {
        title.setText(asset.second.getName());
        assetCategory.setText(asset.second.getDescription());
        icon.setupTokenImageThumbnail(asset.second);

        if (asset.second.isAssetMultiple() || !ERC1155Token.isNFT(asset.first))
        {
            assetCount.setVisibility(View.VISIBLE);
            assetCount.setText(getString(R.string.asset_count_val, asset.second.getSelectedBalance().toString()));
        }

        if (ERC1155Token.isNFT(asset.first))
        {
            tokenId.setVisibility(View.VISIBLE);
            tokenId.setText(getContext().getString(R.string.hash_tokenid, ERC1155Token.getNFTTokenId(asset.first).toString()));
        }
        else
        {
            tokenId.setVisibility(View.GONE);
        }
    }
}
