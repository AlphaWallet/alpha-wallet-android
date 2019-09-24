package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.TokenscriptViewHolder;
import com.alphawallet.app.entity.Token;

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
