package com.alphawallet.app.ui.widget.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.NetworkItem;

import java.util.ArrayList;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

public class SingleSelectNetworkAdapter extends RecyclerView.Adapter<SingleSelectNetworkAdapter.ViewHolder> {
    private final ArrayList<NetworkItem> networkList;
    private boolean hasSelection;

    public SingleSelectNetworkAdapter(ArrayList<NetworkItem> data)
    {
        this.networkList = data;

        for (NetworkItem item : data)
        {
            if (item.isSelected())
            {
                hasSelection = true;
                break;
            }
        }
    }

    public Long getSelectedItem()
    {
        for (NetworkItem data : networkList)
        {
            if (data.isSelected()) return data.getChainId();
        }

        if (networkList.size() > 0)
        {
            return networkList.get(0).getChainId();
        }
        else
        {
            return MAINNET_ID;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int buttonTypeId = R.layout.item_simple_radio;
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(buttonTypeId, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        NetworkItem item = networkList.get(position);
        if (item != null)
        {
            holder.name.setText(item.getName());
            holder.itemLayout.setOnClickListener(v -> clickListener(holder, position));
            holder.checkbox.setSelected(item.isSelected());
        }
    }

    private void clickListener(final ViewHolder holder, final int position)
    {
        for (NetworkItem networkItem : networkList)
        {
            networkItem.setSelected(false);
        }
        networkList.get(position).setSelected(true);
        notifyDataSetChanged();
        holder.checkbox.setSelected(networkList.get(position).isSelected());
    }

    @Override
    public int getItemCount()
    {
        return networkList.size();
    }

    public void selectDefault()
    {
        if (!hasSelection)
        {
            networkList.get(0).setSelected(true);
            notifyItemChanged(0);
        }
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
