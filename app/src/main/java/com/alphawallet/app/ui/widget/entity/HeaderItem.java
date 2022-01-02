package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenSortGroup;
import com.alphawallet.app.ui.widget.holder.HeaderHolder;
import com.alphawallet.app.ui.widget.holder.SearchTokensHolder;


public class HeaderItem extends SortedItem<String> {

    final TokenSortGroup group;

    public HeaderItem(TokenSortGroup group) {
        super(HeaderHolder.VIEW_TYPE, group.data, group.weight);
        this.group = group;
    }

    @Override
    public int compare(SortedItem other) {

        if (other instanceof TokenSortedItem) {
            // if token from the other group = sort by group
            if (group != ((TokenCardMeta)other.value).group) {
                return group.compareTo(((TokenCardMeta)other.value).group);
            } else {
                // token is from the same group, should be after the header
                return -1;
            }
        }

        return weight - other.weight;
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
