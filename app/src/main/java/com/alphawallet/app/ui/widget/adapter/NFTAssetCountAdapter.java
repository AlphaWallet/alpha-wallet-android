package com.alphawallet.app.ui.widget.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.ui.widget.holder.NFTAssetAmountHolder;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by JB on 22/08/2021.
 */
public class NFTAssetCountAdapter extends RecyclerView.Adapter<NFTAssetAmountHolder> {
    private final List<NFTAsset> assets;

    public NFTAssetCountAdapter(List<NFTAsset> data)
    {
        this.assets = data;
    }

    @NotNull
    @Override
    public NFTAssetAmountHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new NFTAssetAmountHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NotNull NFTAssetAmountHolder holder, int position)
    {
        holder.bind(assets.get(position));
    }

    @Override
    public int getItemCount()
    {
        return assets.size();
    }
}
