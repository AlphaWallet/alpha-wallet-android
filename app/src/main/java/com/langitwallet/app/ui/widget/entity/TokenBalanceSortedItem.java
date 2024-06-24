package com.langitwallet.app.ui.widget.entity;

import com.langitwallet.app.entity.tokens.Token;
import com.langitwallet.app.ui.widget.holder.TokenDescriptionHolder;

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
