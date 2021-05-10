package com.alphawallet.app.ui.widget.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.ui.widget.entity.NetworkItem;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectNetworkAdapter extends RecyclerView.Adapter<MultiSelectNetworkAdapter.ViewHolder> {
    private ArrayList<NetworkItem> networkList;
    private boolean hasSelection;

    public MultiSelectNetworkAdapter(ArrayList<NetworkItem> selectedNetworks)
    {
        networkList = selectedNetworks;

        for (NetworkItem item : selectedNetworks)
        {
            // Permanently select Ethereum (when on main net)
            if (CustomViewSettings.isPrimaryNetwork(item))
            {
                item.setSelected(true);
                hasSelection = true;
                break;
            }
        }
    }

    public Integer[] getSelectedItems()
    {
        List<Integer> enabledIds = new ArrayList<>();
        for (NetworkItem data : networkList)
        {
            if (data.isSelected()) enabledIds.add(data.getChainId());
        }

        return enabledIds.toArray(new Integer[0]);
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_simple_check;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);

        return new MultiSelectNetworkAdapter.ViewHolder(itemView);
    }

    public void selectDefault() {
        if (!hasSelection) {
            networkList.get(0).setSelected(true);
            notifyItemChanged(0);
        }
    }

    @Override
    public void onBindViewHolder(MultiSelectNetworkAdapter.ViewHolder holder, int position)
    {
        NetworkItem item = networkList.get(position);

        if (item != null)
        {
            holder.name.setText(item.getName());
            holder.itemLayout.setOnClickListener(v -> clickListener(holder, position));
            holder.checkbox.setSelected(item.isSelected());

            if (networkList.get(position).getName().equals(CustomViewSettings.primaryNetworkName()))
            {
                holder.checkbox.setAlpha(0.5f);
            } else
            {
                holder.checkbox.setAlpha(1.0f);
            }
        }
    }

    private void clickListener(final MultiSelectNetworkAdapter.ViewHolder holder, final int position)
    {
        if (!networkList.get(position).getName().equals(CustomViewSettings.primaryNetworkName()))
        {
            networkList.get(position).setSelected(!networkList.get(position).isSelected());
        }
        holder.checkbox.setSelected(networkList.get(position).isSelected());
    }

    @Override
    public int getItemCount()
    {
        return networkList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView checkbox;
        TextView name;
        View itemLayout;

        ViewHolder(View view)
        {
            super(view);
            checkbox = view.findViewById(R.id.checkbox);
            name = view.findViewById(R.id.name);
            itemLayout = view.findViewById(R.id.layout_list_item);
        }
    }
}