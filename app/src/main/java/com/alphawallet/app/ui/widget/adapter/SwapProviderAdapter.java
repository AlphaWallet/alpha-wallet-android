package com.alphawallet.app.ui.widget.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.ToolDetails;
import com.bumptech.glide.Glide;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class SwapProviderAdapter extends RecyclerView.Adapter<SwapProviderAdapter.ViewHolder>
{
    private final Context context;
    private final List<ToolDetails> displayData;
    private List<String> selectedProviders;

    public SwapProviderAdapter(Context context, List<ToolDetails> data)
    {
        this.context = context;
        displayData = data;
        selectedProviders = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_swap_provider;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        ToolDetails item = displayData.get(position);
        if (item != null)
        {
            holder.title.setText(item.name);
            holder.subtitle.setText(item.url);
            Glide.with(context)
                    .load(item.logoURI)
//                    .error(new ColorDrawable(Color.GRAY))
//                    .fallback(new ColorDrawable(Color.GRAY))
//                    .placeholder(new ColorDrawable(Color.GRAY))
                    .circleCrop()
                    .into(holder.icon);

            holder.layout.setOnClickListener(v -> holder.checkBox.setChecked(!item.isChecked));

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.isChecked = isChecked;
                if (isChecked)
                {
                    selectedProviders.add(item.key);
                }
                else
                {
                    selectedProviders.remove(item.key);
                }
            });

            holder.checkBox.setChecked(item.isChecked);
        }
    }

    public List<String> getSelectedProviders()
    {
        return selectedProviders;
    }

    @Override
    public int getItemCount()
    {
        return displayData.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        RelativeLayout layout;
        ImageView icon;
        TextView title;
        TextView subtitle;
        MaterialCheckBox checkBox;

        ViewHolder(View view)
        {
            super(view);
            layout = view.findViewById(R.id.layout_list_item);
            icon = view.findViewById(R.id.icon);
            title = view.findViewById(R.id.provider);
            subtitle = view.findViewById(R.id.subtitle);
            checkBox = view.findViewById(R.id.checkbox);
        }
    }
}
