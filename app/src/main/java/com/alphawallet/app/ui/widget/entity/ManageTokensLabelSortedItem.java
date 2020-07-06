package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.adapter.TokenListAdapter;

public class ManageTokensLabelSortedItem extends SortedItem<ManageTokensLabelData> {

    public ManageTokensLabelSortedItem(ManageTokensLabelData data, int weight) {
        super(TokenListAdapter.LabelViewHolder.VIEW_TYPE, data, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
