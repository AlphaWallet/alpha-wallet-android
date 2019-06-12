package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.widget.holder.TokenscriptViewHolder;

/**
 * Created by James on 31/05/2019.
 * Stormbird in Sydney
 */
public class TokenscriptSortedItem extends SortedItem<Token>
{
    public TokenscriptSortedItem(Token value)
    {
        super(TokenscriptViewHolder.VIEW_TYPE, value, 0);
    }

    @Override
    public int compare(SortedItem other)
    {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return newItem.viewType == viewType
                || (((TokenscriptSortedItem) newItem).value.getTicketCount() == value.getTicketCount())
                && ((TokenscriptSortedItem) newItem).value.getFullName().equals(value.getFullName());
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
