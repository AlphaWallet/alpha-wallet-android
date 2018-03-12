package com.wallet.crypto.alphawallet.ui.widget.entity;

import com.wallet.crypto.alphawallet.entity.HelpItem;

public class HelpSortedItem extends SortedItem<HelpItem> {

    public HelpSortedItem(int viewType, HelpItem value, int weight) {
        super(viewType, value, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        if (viewType == newItem.viewType) {
            HelpItem helpItem = (HelpItem) newItem.value;
            return value.getQuestion().equals(helpItem.getQuestion());
        }
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return viewType == other.viewType;
    }
}
