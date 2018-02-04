package com.wallet.crypto.trustapp.ui.widget.entity;

import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.holder.TokenHolder;

public class TokenSortedItem extends SortedItem<Token> {

    public TokenSortedItem(Token value, int weight) {
        super(TokenHolder.VIEW_TYPE, value, weight);
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
        return other.viewType == TokenHolder.VIEW_TYPE
                && ((TokenSortedItem) other).value.tokenInfo.address.equalsIgnoreCase(value.tokenInfo.address);
    }
}
