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
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Erc1155AssetListAdapter extends RecyclerView.Adapter<Erc1155AssetListAdapter.ViewHolder> {
    private final List<BigInteger> actualData;
    private final NFTAsset asset;
    private final Context context;
    private final OnAssetClickListener listener;

    public Erc1155AssetListAdapter(Context context, Map<BigInteger, NFTAsset> data, BigInteger tokenId, OnAssetClickListener listener)
    {
        this.context = context;
        this.listener = listener;
        this.asset = data.get(tokenId);
        actualData = new ArrayList<>(data.size());
        for (BigInteger d = BigInteger.ONE; d.compareTo(asset.getBalance().toBigInteger()) <= 0; d = d.add(BigInteger.ONE))
        {
            actualData.add(d);
        }
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_erc1155_asset, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position)
    {
        BigInteger id = actualData.get(position);
        holder.title.setText(asset.getName());
        holder.sequence.setText(id.toString());
        holder.subtitle.setText(asset.getDescription());
        Glide.with(context)
                .load(asset.getImage())
                .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                .into(holder.icon);
        holder.layout.setOnClickListener(v -> listener.onAssetClicked(new Pair<>(id, asset)));
    }

    @Override
    public int getItemCount()
    {
        return actualData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RelativeLayout layout;
        final ImageView icon;
        final TextView title;
        final TextView sequence;
        final TextView subtitle;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.layout);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            sequence = view.findViewById(R.id.sequence);
        }
    }
}
