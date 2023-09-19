package com.alphawallet.app.ui.widget.adapter;


import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.ui.NFTActivity;
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
import io.reactivex.schedulers.Schedulers;

public class NFTAssetsAdapter extends RecyclerView.Adapter<NFTAssetsAdapter.ViewHolder>
{
    private final Activity activity;
    private final OnAssetClickListener listener;
    private final Token token;
    private final boolean isGrid;
    private final OpenSeaService openSeaService;

    private final List<Pair<BigInteger, NFTAsset>> actualData;
    private final List<Pair<BigInteger, NFTAsset>> displayData;
    private String lastFilter;

    public NFTAssetsAdapter(Activity activity, Token token, OnAssetClickListener listener, OpenSeaService openSeaSvs, boolean isGrid)
    {
        this.activity = activity;
        this.listener = listener;
        this.token = token;
        this.isGrid = isGrid;
        this.openSeaService = openSeaSvs;

        actualData = new ArrayList<>();
        switch (token.getInterfaceSpec())
        {
            case ERC721:
            case ERC721_LEGACY:
            case ERC721_TICKET:
            case ERC721_UNDETERMINED:
            case ERC721_ENUMERABLE:
                for (BigInteger i : token.getUniqueTokenIds())
                {
                    NFTAsset asset = token.getAssetForToken(i);
                    actualData.add(new Pair<>(i, asset));
                }
                break;
            case ERC1155:
                Map<BigInteger, NFTAsset> data = token.getCollectionMap();
                for (Map.Entry<BigInteger, NFTAsset> d : data.entrySet())
                {
                    actualData.add(new Pair<>(d.getKey(), d.getValue()));
                }
                break;
        }

        displayData = new ArrayList<>();
        displayData.addAll(actualData);
        lastFilter = "";
        sortData();
    }

    //TODO: Attestations should be attached to the backing Token if available
    public void attachAttestations(Token[] attestations)
    {
        for (Token att : attestations)
        {
            Attestation thisAttn = (Attestation)att;
            NFTAsset attestationAsset = new NFTAsset(thisAttn);
            //displayData.add(new Pair<>(thisAttn.getAttestationUID(), attestationAsset));
        }

        sortData();
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int layoutRes = isGrid ? R.layout.item_erc1155_asset_select_grid : R.layout.item_erc1155_asset_select;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position)
    {
        Pair<BigInteger, NFTAsset> pair = displayData.get(position);
        NFTAsset item = pair.second;
        if (item != null)
        {
            displayAsset(holder, item, pair.first);
        }

        if (item != null && item.requiresReplacement())
        {
            fetchAsset(holder, pair);
        }
    }

    private void displayAsset(@NotNull ViewHolder holder, NFTAsset asset, BigInteger tokenId)
    {
        displayTitle(holder, asset, token, tokenId);
        displayImage(holder, asset);

        holder.layout.setOnClickListener(v -> listener.onAssetClicked(new Pair<>(tokenId, asset)));

        holder.layout.setOnLongClickListener(view -> {
            listener.onAssetLongClicked(new Pair<>(tokenId, asset));
            return false;
        });

        if (isGrid)
        {
            holder.icon.setOnClickListener(v -> listener.onAssetClicked(new Pair<>(tokenId, asset)));
        }
    }

    private void displayImage(@NonNull ViewHolder holder, NFTAsset asset)
    {
        if (asset.hasImageAsset())
        {
            holder.icon.setupTokenImageThumbnail(asset, isGrid);
        }
        else
        {
            holder.icon.showFallbackLayout(token);
        }
    }

    private void displayTitle(@NotNull ViewHolder holder, NFTAsset asset, Token token, BigInteger tokenId)
    {
        int assetCount = asset.isCollection() ? asset.getCollectionCount() : asset.getBalance().intValue();
        int textId = assetCount == 1 ? R.string.asset_description_text : R.string.asset_description_text_plural;

        String title = asset.getName() == null ? String.format("ID #%s", tokenId) : asset.getName();
        holder.title.setText(title);

        // set subtitle
        if (token.isERC721())
        {
            // Hide subtitle containing redundant information
            holder.subtitle.setVisibility(View.GONE);
        }
        else
        {
            holder.subtitle.setText(activity.getString(textId, assetCount, asset.getAssetCategory(tokenId).getValue()));
            holder.subtitle.setVisibility(View.VISIBLE);
        }
    }

    private void fetchAsset(ViewHolder holder, Pair<BigInteger, NFTAsset> pair)
    {
        if (EthereumNetworkBase.hasOpenseaAPI(token.tokenInfo.chainId))
        {
            pair.second.metaDataLoader = openSeaService.getAsset(token, pair.first)
                    .map(NFTAsset::new)
                    .map(asset -> storeAsset(pair.first, asset, pair.second))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(asset -> checkAsset(asset, holder, pair), e -> {});
        }
        else
        {
            fetchContractMetadata(holder, pair);
        }
    }

    private void fetchContractMetadata(ViewHolder holder, Pair<BigInteger, NFTAsset> pair)
    {
        pair.second.metaDataLoader = Single.fromCallable(() -> {
                    return token.fetchTokenMetadata(pair.first); //fetch directly from token
                }).map(newAsset -> storeAsset(pair.first, newAsset, pair.second))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(a -> displayAsset(holder, a, pair.first), e -> {});
    }

    private void checkAsset(NFTAsset asset, ViewHolder holder, Pair<BigInteger, NFTAsset> pair)
    {
        if (asset.hasImageAsset())
        {
            displayAsset(holder, asset, pair.first);
        }
        else
        {
            fetchContractMetadata(holder, pair);
        }
    }

    private NFTAsset storeAsset(BigInteger tokenId, NFTAsset fetchedAsset, NFTAsset oldAsset)
    {
        if (!fetchedAsset.hasImageAsset()) return oldAsset;
        fetchedAsset.updateFromRaw(oldAsset);
        if (activity != null && activity instanceof NFTActivity)
        {
            ((NFTActivity) activity).storeAsset(tokenId, fetchedAsset);
        }

        token.addAssetToTokenBalanceAssets(tokenId, fetchedAsset);
        return fetchedAsset;
    }

    @Override
    public int getItemCount()
    {
        return displayData.size();
    }

    private void sortData()
    {
        Collections.sort(displayData, (e1, e2) -> {
            BigInteger tokenId1 = e1.first;
            BigInteger tokenId2 = e2.first;
            return tokenId1.compareTo(tokenId2);
        });
    }

    public void updateList(List<Pair<BigInteger, NFTAsset>> list)
    {
        displayData.clear();
        displayData.addAll(list);
        sortData();
        notifyDataSetChanged();
    }

    public void filter(String searchFilter)
    {
        if (lastFilter.equalsIgnoreCase(searchFilter)) //avoid search update if not required
        {
            return;
        }

        List<Pair<BigInteger, NFTAsset>> filteredList = new ArrayList<>();
        for (Pair<BigInteger, NFTAsset> data : actualData)
        {
            NFTAsset asset = data.second;
            if (asset.getName() != null)
            {
                if (asset.getName().toLowerCase().contains(searchFilter.toLowerCase()))
                {
                    filteredList.add(data);
                }
            }
            else
            {
                String id = data.first.toString();
                if (id.contains(searchFilter))
                {
                    filteredList.add(data);
                }
            }
        }
        updateList(filteredList);
        lastFilter = searchFilter;
    }

    @Override
    public int getItemViewType(int position)
    {
        return position;
    }

    public void onDestroy()
    {
        //clear all loaders
        for (Pair<BigInteger, NFTAsset> assetPair : displayData)
        {
            if (assetPair.second != null && assetPair.second.metaDataLoader != null && !assetPair.second.metaDataLoader.isDisposed())
            {
                assetPair.second.metaDataLoader.dispose();
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        ViewGroup layout;
        NFTImageView icon;
        TextView title;
        TextView subtitle;
        ImageView arrowRight;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.holding_view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);

            arrowRight = view.findViewById(R.id.arrow_right);
            if (arrowRight != null) arrowRight.setVisibility(View.VISIBLE);

        }
    }
}
