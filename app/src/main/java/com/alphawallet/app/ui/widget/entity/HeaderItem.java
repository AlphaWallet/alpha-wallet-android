package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenSortGroup;
import com.alphawallet.app.ui.widget.holder.HeaderHolder;
import com.alphawallet.app.ui.widget.holder.SearchTokensHolder;


public class HeaderItem extends SortedItem<TokenGroup> {

    public HeaderItem(TokenGroup group) {
        super(HeaderHolder.VIEW_TYPE, group, new TokenPosition(group, 1, 1, true));
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
