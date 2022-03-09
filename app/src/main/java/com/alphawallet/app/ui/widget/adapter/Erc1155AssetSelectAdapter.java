package com.alphawallet.app.ui.widget.adapter;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.ui.widget.NonFungibleAdapterInterface;
import com.alphawallet.app.ui.widget.OnAssetSelectListener;
import com.alphawallet.app.widget.NFTImageView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Erc1155AssetSelectAdapter extends RecyclerView.Adapter<Erc1155AssetSelectAdapter.ViewHolder> implements NonFungibleAdapterInterface
{
    private final List<Pair<BigInteger, NFTAsset>> actualData;
    private final Context context;
    private final OnAssetSelectListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public Erc1155AssetSelectAdapter(Context context, Map<BigInteger, NFTAsset> data, OnAssetSelectListener listener)
    {
        this.context = context;
        this.listener = listener;
        this.actualData = new ArrayList<>(data.size());
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
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Pair<BigInteger, NFTAsset> pair = actualData.get(position);
        NFTAsset item = pair.second;
        if (item != null)
        {
            holder.title.setText(item.getName());
            holder.assetCategory.setText(item.getDescription());
            holder.icon.setupTokenImageThumbnail(item);
            holder.checkBox.setChecked(item.isSelected());
            holder.holderLayout.setOnClickListener(v -> {
                boolean b = !item.isSelected();
                setSelected(position, b);
                holder.checkBox.setChecked(b);
            });

            if (ERC1155Token.isNFT(pair.first))
            {
                holder.tokenId.setVisibility(View.VISIBLE);
                holder.tokenId.setText(context.getString(R.string.hash_tokenid, ERC1155Token.getNFTTokenId(pair.first).toString()));
            }
            else
            {
                holder.tokenId.setVisibility(View.GONE);
            }

            if (item.isAssetMultiple() || !ERC1155Token.isNFT(pair.first))
            {
                holder.assetCount.setVisibility(View.VISIBLE);
                holder.assetCount.setText(context.getString(R.string.asset_count_val, item.getBalance().toString()));

                if (item.getSelectedBalance().compareTo(BigDecimal.ZERO) > 0)
                {
                    holder.selectionAmount.setVisibility(View.VISIBLE);
                    holder.selectionAmount.setText(item.getSelectedBalance().toString());
                    holder.selectionAmount.setScaleX(0.0f);
                    holder.selectionAmount.setScaleY(0.0f);
                    holder.selectionAmount.setAlpha(0.0f);
                    holder.selectionAmount.animate().alpha(1.0f).setDuration(300);
                    holder.selectionAmount.animate().scaleX(1.0f).setDuration(300);
                    holder.selectionAmount.animate().scaleY(1.0f).setDuration(300);
                }
                else
                {
                    holder.selectionAmount.animate().alpha(0.0f).setDuration(300);
                    holder.selectionAmount.animate().scaleX(0.0f).setDuration(300);
                    holder.selectionAmount.animate().scaleY(0.0f).setDuration(300);
                    handler.postDelayed(() -> holder.selectionAmount.setVisibility(View.GONE), 500);
                }
                return;
            }
            else
            {
                holder.assetCount.setVisibility(View.GONE);
            }

            holder.selectionAmount.setVisibility(View.GONE);
        }
    }

    private void setSelected(int position, boolean selected) {
        NFTAsset asset = actualData.get(position).second;
        asset.setSelected(selected);
        if (selected)
        {
            listener.onAssetSelected(actualData.get(position).first, asset, position);
        }
        else
        {
            listener.onAssetUnselected();
            asset.setSelectedBalance(BigDecimal.ZERO);
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemCount()
    {
        return actualData.size();
    }

    public ArrayList<NFTAsset> getSelectedAssets()
    {
        ArrayList<NFTAsset> selectedAssets = new ArrayList<>();

        for (Pair<BigInteger, NFTAsset> asset : actualData) {
            if (asset.second.isSelected()) {
                selectedAssets.add(asset.second);
            }
        }

        return selectedAssets;
    }

    public List<BigInteger> getSelectedTokenIds()
    {
        List<BigInteger> selectedIds = new ArrayList<>();

        for (Pair<BigInteger, NFTAsset> asset : actualData)
        {
            if (asset.second.isSelected() && asset.second.getSelectedBalance().compareTo(BigDecimal.ZERO) > 0)
            {
                selectedIds.add(asset.first);
            }
        }

        return selectedIds;
    }

    @Override
    public List<BigInteger> getSelectedTokenIds(List<BigInteger> selection)
    {
        List<BigInteger> tokenIds = new ArrayList<>();
        for (Pair<BigInteger, NFTAsset> asset : actualData)
        {
            if (asset.second.isSelected() && asset.second.getSelectedBalance().compareTo(BigDecimal.ZERO) > 0)
            {
                tokenIds.add(asset.first);
            }
        }

        return tokenIds;
    }

    @Override
    public int getSelectedGroups()
    {
        return 0;
    }

    @Override
    public void setRadioButtons(boolean selected)
    {
        //No action, already handled
    }

    public int getSelectedAmount(int position)
    {
        if (position < actualData.size())
        {
            int amt = actualData.get(position).second.getSelectedBalance().intValue();
            return amt > 0 ? amt : 1;
        }

        return 1;
    }

    public void setSelectedAmount(int position, int amount)
    {
        if (position < actualData.size())
        {
            NFTAsset asset = actualData.get(position).second;
            if (amount == 0)
            {
                asset.setSelected(false);
            }
            else if (amount > asset.getBalance().intValue())
            {
                amount = asset.getBalance().intValue();
            }
            asset.setSelectedBalance(BigDecimal.valueOf(amount));
            notifyItemChanged(position);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RelativeLayout holderLayout;
        final NFTImageView icon;
        final TextView title;
        final TextView assetCategory;
        final TextView assetCount;
        final TextView selectionAmount;
        final CheckBox checkBox;
        final TextView tokenId;

        ViewHolder(View view)
        {
            super(view);
            holderLayout = view.findViewById(R.id.holding_view);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            assetCategory = view.findViewById(R.id.subtitle);
            assetCount = view.findViewById(R.id.count);
            checkBox = view.findViewById(R.id.checkbox);
            selectionAmount = view.findViewById(R.id.text_count);
            tokenId = view.findViewById(R.id.token_id);

            checkBox.setVisibility(View.VISIBLE);
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
