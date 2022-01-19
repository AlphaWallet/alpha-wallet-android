package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.TokenDescriptionHolder;
import com.alphawallet.app.entity.tokens.Token;

/**
 * Created by James on 12/02/2018.
 */

public class TokenBalanceSortedItem extends SortedItem<Token> {

    public TokenBalanceSortedItem(Token value) {
        super(TokenDescriptionHolder.VIEW_TYPE, value, new TokenPosition(0));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return newItem.viewType == viewType
                || (((TokenBalanceSortedItem) newItem).value.getTokenCount() == value.getTokenCount())
                && ((TokenBalanceSortedItem) newItem).value.getFullName().equals(value.getFullName());
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return other.viewType == viewType;
    }
}
