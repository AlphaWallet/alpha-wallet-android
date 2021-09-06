package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.HeaderHolder;
import com.alphawallet.app.ui.widget.holder.SearchTokensHolder;


public class HeaderItem extends SortedItem<String> {

    public HeaderItem(String data, int weight) {
        super(HeaderHolder.VIEW_TYPE, data, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return other.viewType == viewType;
    }
}
