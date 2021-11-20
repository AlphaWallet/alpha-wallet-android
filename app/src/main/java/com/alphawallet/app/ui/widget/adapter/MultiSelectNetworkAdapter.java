package com.alphawallet.app.ui.widget.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.NetworkItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectNetworkAdapter extends RecyclerView.Adapter<MultiSelectNetworkAdapter.ViewHolder> {
    private final List<NetworkItem> networkList;
    private boolean hasClicked = false;

    public interface EditNetworkListener {
        void onEditNetwork(long chainId, View parent);
    }

    private final EditNetworkListener editListener;


    public MultiSelectNetworkAdapter(List<NetworkItem> selectedNetworks, EditNetworkListener editNetworkListener)
    {
        networkList = selectedNetworks;
        editListener = editNetworkListener;
    }

    public Long[] getSelectedItems()
    {
        List<Long> enabledIds = new ArrayList<>();
        for (NetworkItem data : networkList)
        {
            if (data.isSelected()) enabledIds.add(data.getChainId());
        }

        return enabledIds.toArray(new Long[0]);
    }

    public boolean hasSelectedItems()
    {
        return hasClicked;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_simple_check;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);

        return new MultiSelectNetworkAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MultiSelectNetworkAdapter.ViewHolder holder, int position)
    {
        NetworkItem item = networkList.get(position);

        if (item != null)
        {
            holder.name.setText(item.getName());
            holder.itemLayout.setOnClickListener(v -> clickListener(holder, position));
            holder.manageView.setVisibility(View.VISIBLE);
            holder.manageView.setOnClickListener(v ->  editListener.onEditNetwork(networkList.get(position).getChainId(), holder.manageView));
            holder.checkbox.setSelected(item.isSelected());
        }
    }

    private void clickListener(final MultiSelectNetworkAdapter.ViewHolder holder, final int position)
    {
        networkList.get(position).setSelected(!networkList.get(position).isSelected());
        holder.checkbox.setSelected(networkList.get(position).isSelected());
        hasClicked = true;
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
        View manageView;

        ViewHolder(View view)
        {
            super(view);
            checkbox = view.findViewById(R.id.checkbox);
            name = view.findViewById(R.id.name);
            itemLayout = view.findViewById(R.id.layout_list_item);
            manageView = view.findViewById(R.id.manage_btn);
        }
    }
}