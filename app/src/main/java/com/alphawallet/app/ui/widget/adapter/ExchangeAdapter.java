package com.alphawallet.app.ui.widget.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.lifi.ToolDetails;
import com.alphawallet.app.widget.AddressIcon;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

public class ExchangeAdapter extends RecyclerView.Adapter<ExchangeAdapter.ViewHolder>
{
    private final List<ToolDetails> data;

    public ExchangeAdapter(List<ToolDetails> data)
    {
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
        ToolDetails item = data.get(position);
        if (item != null)
        {
            holder.title.setText(item.name);

            holder.subtitle.setText(item.url);

            holder.icon.bindData(item.logoURI, -1, "", "");

            holder.layout.setOnClickListener(v -> holder.checkBox.setChecked(!item.isChecked));

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.isChecked = isChecked);

            holder.checkBox.setChecked(item.isChecked);
        }
    }

    public List<ToolDetails> getExchanges()
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
        AddressIcon icon;
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
