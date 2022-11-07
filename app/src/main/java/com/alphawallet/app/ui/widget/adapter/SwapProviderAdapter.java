package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.SwapProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

public class SwapProviderAdapter extends RecyclerView.Adapter<SwapProviderAdapter.ViewHolder>
{
    private final List<SwapProvider> data;
    private final Context context;

    public SwapProviderAdapter(Context context, List<SwapProvider> data)
    {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_exchange;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        SwapProvider item = data.get(position);
        if (item != null)
        {
            holder.title.setText(item.name);

            holder.subtitle.setText(item.url);

            Glide.with(context)
                    .load(item.logoURI)
                    .placeholder(R.drawable.ic_logo)
                    .circleCrop()
                    .into(new DrawableImageViewTarget(holder.icon));

            holder.layout.setOnClickListener(v -> holder.checkBox.setChecked(!item.isChecked));

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.isChecked = isChecked);

            holder.checkBox.setChecked(item.isChecked);
        }
    }

    public List<SwapProvider> getExchanges()
    {
        return data;
    }

    @Override
    public int getItemCount()
    {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout layout;
        AppCompatImageView icon;
        TextView title;
        TextView subtitle;
        MaterialCheckBox checkBox;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.layout_list_item);
            icon = view.findViewById(R.id.token_icon);
            title = view.findViewById(R.id.provider);
            subtitle = view.findViewById(R.id.subtitle);
            checkBox = view.findViewById(R.id.checkbox);
        }
    }
}
