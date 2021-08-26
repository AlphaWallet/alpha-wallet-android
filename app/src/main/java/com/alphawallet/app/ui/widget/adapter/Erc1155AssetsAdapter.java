package com.alphawallet.app.ui.widget.adapter;


import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.alphawallet.app.widget.NFTImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Erc1155AssetsAdapter extends RecyclerView.Adapter<Erc1155AssetsAdapter.ViewHolder> {
    private final List<Pair<BigInteger, NFTAsset>> actualData;
    private final Context context;
    private final OnAssetClickListener listener;

    public Erc1155AssetsAdapter(Context context, Map<BigInteger, NFTAsset> data, OnAssetClickListener listener)
    {
        this.context = context;
        this.listener = listener;
        actualData = new ArrayList<>(data.size());
        for (Map.Entry<BigInteger, NFTAsset> d : data.entrySet())
        {
            actualData.add(new Pair<>(d.getKey(), d.getValue()));
        }

        sortData();
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_erc1155_asset_select, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position)
    {
        Pair<BigInteger, NFTAsset> pair = actualData.get(position);
        NFTAsset item = pair.second;
        if (item != null)
        {
            int assetCount = item.isCollection() ? item.getCollectionCount() : item.getBalance().intValue();
            int textId = assetCount == 1 ? R.string.asset_description_text : R.string.asset_description_text_plural;
            holder.title.setText(item.getName());
            holder.subtitle.setText(context.getString(textId, assetCount, item.getAssetCategory()));
            holder.icon.setupTokenImageThumbnail(item);
            holder.layout.setOnClickListener(v -> listener.onAssetClicked(pair));
        }
    }

    @Override
    public int getItemCount()
    {
        return actualData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout layout;
        NFTImageView icon;
        TextView title;
        TextView subtitle;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.holding_view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);

            view.findViewById(R.id.arrow_right).setVisibility(View.VISIBLE);
        }
    }

    private void sortData()
    {
        Collections.sort(actualData, (e1, e2) -> {
            BigInteger tokenId1 = e1.first;
            BigInteger tokenId2 = e2.first;
            return tokenId1.compareTo(tokenId2);
        });
    }
}
