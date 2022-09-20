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

import java.util.ArrayList;
import java.util.List;

public class ExchangeAdapter extends RecyclerView.Adapter<ExchangeAdapter.ViewHolder>
{
    private final List<ToolDetails> displayData;
    private final List<String> selectedExchanges;

    public ExchangeAdapter(List<ToolDetails> data)
    {
        displayData = data;
        selectedExchanges = new ArrayList<>();
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
        ToolDetails item = displayData.get(position);
        if (item != null)
        {
            holder.title.setText(item.name);

            holder.subtitle.setText(item.url);

            holder.icon.bindData(item.logoURI, -1, "", "");

            holder.layout.setOnClickListener(v -> holder.checkBox.setChecked(!item.isChecked));

            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.isChecked = isChecked;
                if (isChecked)
                {
                    selectedExchanges.add(item.key);
                }
                else
                {
                    selectedExchanges.remove(item.key);
                }
            });

            holder.checkBox.setChecked(item.isChecked);
        }
    }

    public List<String> getSelectedExchanges()
    {
        return selectedExchanges;
    }

    @Override
    public int getItemCount()
    {
        return displayData.size();
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
