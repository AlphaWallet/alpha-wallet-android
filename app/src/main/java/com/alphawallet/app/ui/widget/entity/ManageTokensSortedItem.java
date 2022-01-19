package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.ManageTokensHolder;


public class ManageTokensSortedItem extends SortedItem<ManageTokensData> {

    public ManageTokensSortedItem(ManageTokensData data, int weight) {
        super(ManageTokensHolder.VIEW_TYPE, data, weight);
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
