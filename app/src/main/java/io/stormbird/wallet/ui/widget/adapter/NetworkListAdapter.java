package io.stormbird.wallet.ui.widget.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.entity.NetworkItem;

public class NetworkListAdapter extends ArrayAdapter<NetworkItem> {
    private ArrayList<NetworkItem> dataSet;
    private String selectedItem;
    private int chainId;
    private boolean singleItem;

    private void setSelectedItem(String selectedItem, int chainId) {
        this.selectedItem = selectedItem;
        this.chainId = chainId;
    }

    private int getSelectedChainId() {
        return this.chainId;
    }

    public Integer[] getSelectedItems() {
        List<Integer> enabledIds = new ArrayList<>();
        for (NetworkItem data : dataSet) {
            if (data.isSelected()) enabledIds.add(data.getChainId());
        }

        return enabledIds.toArray(new Integer[0]);
    }

    private class ViewHolder {
        ImageView checkbox;
        TextView name;
        LinearLayout itemLayout;
    }

    public NetworkListAdapter(Context context, ArrayList<NetworkItem> data, String selectedItem, boolean singleItem) {
        super(context, R.layout.item_dialog_list, data);
        this.dataSet = data;
        this.selectedItem = selectedItem;
        this.singleItem = singleItem;

        if (!singleItem) {
            for (NetworkItem item : data) {
                if (item.getName().equals(C.ETHEREUM_NETWORK_NAME)) {
                    item.setSelected(true);
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NetworkItem item = getItem(position);
        final NetworkListAdapter.ViewHolder viewHolder;
        View view = convertView;

        if (view == null) {
            viewHolder = new NetworkListAdapter.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.item_dialog_list, null);
            view.setTag(viewHolder);
            viewHolder.name = view.findViewById(R.id.name);
            viewHolder.checkbox = view.findViewById(R.id.checkbox);
            viewHolder.itemLayout = view.findViewById(R.id.layout_list_item);
        } else {
            viewHolder = (NetworkListAdapter.ViewHolder) view.getTag();
        }

        if (item != null) {
            viewHolder.name.setText(item.getName());
            viewHolder.itemLayout.setOnClickListener(v -> {
                if (singleItem) {
                    for (NetworkItem networkItem : dataSet) {
                        networkItem.setSelected(false);
                    }
                    dataSet.get(position).setSelected(true);
                } else if (!dataSet.get(position).getName().equals(C.ETHEREUM_NETWORK_NAME)) {
                    if (dataSet.get(position).isSelected()) {
                        dataSet.get(position).setSelected(false);
                    } else {
                        dataSet.get(position).setSelected(true);
                    }
                }
                setSelectedItem(dataSet.get(position).getName(), dataSet.get(position).getChainId());
                notifyDataSetChanged();
            });

            if (item.isSelected()) {
                int resource = singleItem ? R.drawable.ic_checkbox_active : R.drawable.button_square_checked;
                viewHolder.checkbox.setImageResource(resource);
            } else {
                int resource = singleItem ? R.drawable.ic_checkbox : R.drawable.button_square_unchecked;
                viewHolder.checkbox.setImageResource(resource);
            }
        }

        return view;
    }
}