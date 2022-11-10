package com.alphawallet.app.ui.widget.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.ui.widget.entity.NetworkItem;
import com.alphawallet.app.widget.TokenIcon;
import com.google.android.material.checkbox.MaterialCheckBox;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectNetworkAdapter extends RecyclerView.Adapter<MultiSelectNetworkAdapter.ViewHolder>
{
    private final List<NetworkItem> networkList;
    private final Callback callback;
    private boolean hasClicked = false;

    public MultiSelectNetworkAdapter(List<NetworkItem> selectedNetworks, Callback callback)
    {
        networkList = selectedNetworks;
        this.callback = callback;
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
        int buttonTypeId = R.layout.item_network_check;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MultiSelectNetworkAdapter.ViewHolder holder, int position)
    {
        NetworkItem item = networkList.get(position);

        if (item != null)
        {
            holder.name.setText(item.getName());
            holder.chainId.setText(holder.itemLayout.getContext().getString(R.string.chain_id, item.getChainId()));
            holder.itemLayout.setOnClickListener(v -> clickListener(holder, position));
            holder.manageView.setVisibility(View.VISIBLE);
            holder.manageView.setOnClickListener(v -> callback.onEditSelected(networkList.get(position).getChainId(), holder.manageView));
            holder.checkbox.setChecked(item.isSelected());
            holder.tokenIcon.bindData(item.getChainId());

            if (EthereumNetworkBase.isNetworkDeprecated(item.getChainId()))
            {
                holder.deprecatedIndicator.setVisibility(View.VISIBLE);
                holder.tokenIcon.setGrayscale(true);
                holder.name.setAlpha(0.7f);
                holder.chainId.setAlpha(0.7f);
            }
        }
    }

    public int getSelectedItemCount()
    {
        return getSelectedItems().length;
    }

    private void clickListener(final MultiSelectNetworkAdapter.ViewHolder holder, final int position)
    {
        networkList.get(position).setSelected(!networkList.get(position).isSelected());
        holder.checkbox.setChecked(networkList.get(position).isSelected());
        hasClicked = true;
        callback.onCheckChanged(networkList.get(position).getChainId(), getSelectedItemCount());
    }

    @Override
    public int getItemCount()
    {
        return networkList.size();
    }

    public interface Callback
    {
        void onEditSelected(long chainId, View parent);

        void onCheckChanged(long chainId, int count);
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        MaterialCheckBox checkbox;
        TextView name;
        View itemLayout;
        View manageView;
        TokenIcon tokenIcon;
        TextView chainId;
        TextView deprecatedIndicator;

        ViewHolder(View view)
        {
            super(view);
            checkbox = view.findViewById(R.id.checkbox);
            name = view.findViewById(R.id.name);
            itemLayout = view.findViewById(R.id.layout_list_item);
            manageView = view.findViewById(R.id.manage_btn);
            tokenIcon = view.findViewById(R.id.token_icon);
            chainId = view.findViewById(R.id.chain_id);
            deprecatedIndicator = view.findViewById(R.id.deprecated);
        }
    }
}
