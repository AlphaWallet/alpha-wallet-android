package com.wallet.crypto.alphawallet.ui.widget.entity;

import com.wallet.crypto.alphawallet.ui.widget.holder.TicketHolder;

/**
 * Created by James on 10/02/2018.
 */

public class TokenIdSortedItem<T> extends SortedItem<Integer>
{
    public TokenIdSortedItem(int tokenId, int weight) {
        super(TicketHolder.VIEW_TYPE, tokenId, weight);
    }

    @Override
    public int compare(SortedItem other)
    {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == TicketHolder.VIEW_TYPE
                && ((TokenIdSortedItem) other).value == value;
    }
}
