package com.alphawallet.app.ui.widget.adapter;


import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.alphawallet.app.widget.NFTImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class Erc1155AssetListAdapter extends RecyclerView.Adapter<Erc1155AssetListAdapter.ViewHolder> {
    private final List<BigInteger> actualData;
    private final Map<BigInteger, NFTAsset> assetData;
    private final Context context;
    private final OnAssetClickListener listener;

    public Erc1155AssetListAdapter(Context context, Map<BigInteger, NFTAsset> data, NFTAsset asset, OnAssetClickListener listener)
    {
        this.context = context;
        this.listener = listener;
        this.assetData = data;
        this.actualData = asset.getCollectionIds();
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_erc1155_asset_select, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position)
    {
        BigInteger id = actualData.get(position);
        holder.title.setText(assetData.get(id).getName());
        holder.tokenId.setText(context.getString(R.string.hash_tokenid, ERC1155Token.getNFTTokenId(id).toString())); //base value of token
        holder.subtitle.setText(assetData.get(id).getDescription());
        holder.icon.setupTokenImageThumbnail(assetData.get(id));
        holder.layout.setOnClickListener(v -> listener.onAssetClicked(new Pair<>(id, assetData.get(id))));
    }

    @Override
    public int getItemCount()
    {
        return actualData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RelativeLayout layout;
        final NFTImageView icon;
        final TextView title;
        final TextView tokenId;
        final TextView subtitle;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.holding_view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            tokenId = view.findViewById(R.id.token_id);
            tokenId.setVisibility(View.VISIBLE);

            view.findViewById(R.id.arrow_right).setVisibility(View.VISIBLE);
        }
    }
}
