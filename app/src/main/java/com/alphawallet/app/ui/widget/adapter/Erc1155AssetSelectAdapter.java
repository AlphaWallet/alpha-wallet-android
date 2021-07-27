package com.alphawallet.app.ui.widget.adapter;


import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.ERC1155Asset;
import com.alphawallet.app.ui.widget.OnAssetSelectListener;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Erc1155AssetSelectAdapter extends RecyclerView.Adapter<Erc1155AssetSelectAdapter.ViewHolder> {
    private List<Pair<Long, ERC1155Asset>> actualData;
    private Context context;
    private OnAssetSelectListener listener;

    public Erc1155AssetSelectAdapter(Context context, Map<Long, ERC1155Asset> data, OnAssetSelectListener listener)
    {
        this.context = context;
        this.listener = listener;
        actualData = new ArrayList<>(data.size());
        for (Map.Entry<Long, ERC1155Asset> d : data.entrySet()) {
            actualData.add(new Pair<>(d.getKey(), d.getValue()));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_erc1155_asset_select, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        Pair<Long, ERC1155Asset> pair = actualData.get(position);
        ERC1155Asset item = pair.second;
        if (item != null)
        {
            holder.title.setText(item.getTitle());
            holder.subtitle.setText(item.getSubtitle());
            Glide.with(context)
                    .load(item.getIconUri())
                    .apply(new RequestOptions().placeholder(R.drawable.ic_logo))
                    .into(holder.icon);
            holder.checkBox.setChecked(item.isSelected());
            holder.infoLayout.setOnClickListener(v -> {
                boolean b = !item.isSelected();
                setSelected(position, b);
                holder.checkBox.setChecked(b);
            });
        }
    }

    private void setSelected(int position, boolean selected) {
        actualData.get(position).second.setSelected(selected);
        if (selected) listener.onAssetSelected();
        else listener.onAssetUnselected();
    }

    @Override
    public int getItemCount()
    {
        return actualData.size();
    }

    public List<ERC1155Asset> getSelectedAssets()
    {
        List<ERC1155Asset> selectedAssets = new ArrayList<>();

        for (Pair<Long, ERC1155Asset> asset : actualData) {
            if (asset.second.isSelected()) {
                selectedAssets.add(asset.second);
            }
        }

        return selectedAssets;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout menuLayout;
        LinearLayout infoLayout;
        ImageView icon;
        TextView title;
        TextView subtitle;
        CheckBox checkBox;

        ViewHolder(View view)
        {
            super(view);
            menuLayout = view.findViewById(R.id.layout_menu);
            infoLayout = view.findViewById(R.id.layout_info);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            checkBox = view.findViewById(R.id.checkbox);
        }
    }
}
