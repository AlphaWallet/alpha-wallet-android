package io.awallet.crypto.alphawallet.ui.widget.entity;

import io.awallet.crypto.alphawallet.ui.widget.holder.TicketHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;

/**
 * Created by James on 10/02/2018.
 */

public class TokenIdSortedItem extends SortedItem<TicketRange>
{
    public TokenIdSortedItem(TicketRange range, int weight) {
        super(TicketHolder.VIEW_TYPE, range, weight);
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
                && ( ((TokenIdSortedItem) other).value.tokenIds.size() == value.tokenIds.size()
                && ((TokenIdSortedItem) other).value.tokenIds.get(0) == value.tokenIds.get(0));
    }
}
