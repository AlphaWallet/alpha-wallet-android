package com.langitwallet.app.ui.widget.entity;

import com.langitwallet.app.entity.tokendata.TokenGroup;
import com.langitwallet.app.ui.widget.holder.HeaderHolder;


public class HeaderItem extends SortedItem<TokenGroup> {

    public HeaderItem(TokenGroup group) {
        super(HeaderHolder.VIEW_TYPE, group, new TokenPosition(group, 1, 3, true));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return newItem.value.equals(value);
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return other.viewType == viewType && other.value.equals(value);
    }
}
