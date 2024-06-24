package com.langitwallet.app.ui.widget.entity;

import com.langitwallet.app.ui.widget.holder.ManageTokensHolder;


public class ManageTokensSortedItem extends SortedItem<ManageTokensData> {

    public ManageTokensSortedItem(ManageTokensData data) {
        super(ManageTokensHolder.VIEW_TYPE, data, new TokenPosition(Long.MAX_VALUE));
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
