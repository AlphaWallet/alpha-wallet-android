package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.SearchTokensHolder;


public class ManageTokensSearchItem extends SortedItem<ManageTokensData> {

    public ManageTokensSearchItem(ManageTokensData data, int weight) {
        super(SearchTokensHolder.VIEW_TYPE, data, new TokenPosition(weight));
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
