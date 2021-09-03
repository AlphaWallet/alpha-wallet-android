package com.alphawallet.app.ui.widget.adapter;


import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.Erc1155Activity;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.alphawallet.app.widget.NFTImageView;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Erc1155AssetsAdapter extends RecyclerView.Adapter<Erc1155AssetsAdapter.ViewHolder> {
    private final List<Pair<BigInteger, NFTAsset>> actualData;
    private final Activity activity;
    private final OnAssetClickListener listener;
    private final Token token;

    public Erc1155AssetsAdapter(Activity activity, Token token, Map<BigInteger, NFTAsset> data, OnAssetClickListener listener)
    {
        this.activity = activity;
        this.listener = listener;
        this.token = token;
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
            displayAsset(holder, item, pair.first);
        }

        if (item != null && item.requiresReplacement())
        {
            //fetch asset
            fetchAsset(holder, pair);
            holder.loadingSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void displayAsset(@NotNull ViewHolder holder, NFTAsset asset, BigInteger tokenId)
    {
        int assetCount = asset.isCollection() ? asset.getCollectionCount() : asset.getBalance().intValue();
        int textId = assetCount == 1 ? R.string.asset_description_text : R.string.asset_description_text_plural;
        holder.title.setText(asset.getName());
        holder.subtitle.setText(activity.getString(textId, assetCount, asset.getAssetCategory()));
        holder.icon.setupTokenImageThumbnail(asset);
        holder.layout.setOnClickListener(v -> listener.onAssetClicked(new Pair<>(tokenId, asset)));
        holder.loadingSpinner.setVisibility(View.GONE);
    }

    private void fetchAsset(ViewHolder holder, Pair<BigInteger, NFTAsset> pair)
    {
        holder.assetLoader = Single.fromCallable(() -> {
            return token.fetchTokenMetadata(pair.first); //fetch directly from token
        }).map(newAsset -> storeAsset(pair.first, newAsset, pair.second))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(a -> displayAsset(holder, a, pair.first), e -> { });
    }

    private NFTAsset storeAsset(BigInteger tokenId, NFTAsset fetchedAsset, NFTAsset oldAsset)
    {
        fetchedAsset.updateFromRaw(oldAsset);
        if (activity != null && activity instanceof Erc1155Activity)
        {
            ((Erc1155Activity)activity).storeAsset(tokenId, fetchedAsset);
        }

        token.addAssetToTokenBalanceAssets(tokenId, fetchedAsset);
        return fetchedAsset;
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
        ProgressBar loadingSpinner;

        @Nullable
        private Disposable assetLoader;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.holding_view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            loadingSpinner = view.findViewById(R.id.loading_spinner);

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
