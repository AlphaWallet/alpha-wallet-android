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
import com.alphawallet.app.entity.tokens.ERC1155Asset;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Erc1155AssetsAdapter extends RecyclerView.Adapter<Erc1155AssetsAdapter.ViewHolder> {
    private List<Pair<Long, ERC1155Asset>> actualData;
    private Context context;
    private OnAssetClickListener listener;

    public Erc1155AssetsAdapter(Context context, Map<Long, ERC1155Asset> data, OnAssetClickListener listener)
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
                .inflate(R.layout.item_erc1155_asset, parent, false);
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
            holder.layout.setOnClickListener(v -> listener.onAssetClicked(item));
        }
    }

    @Override
    public int getItemCount()
    {
        return actualData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout layout;
        ImageView icon;
        TextView title;
        TextView subtitle;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.layout);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
        }
    }
}
