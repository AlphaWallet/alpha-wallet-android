package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.QuantitySelectorHolder;
import com.alphawallet.app.entity.tokens.Token;

/**
 * Created by James on 28/02/2018.
 */

public class QuantitySelectorSortedItem extends SortedItem<Token>
{
    public QuantitySelectorSortedItem(Token value)
    {
        super(QuantitySelectorHolder.VIEW_TYPE, value, 0);
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
                || (((TokenBalanceSortedItem) newItem).value.getTokenCount() == value.getTokenCount())
                && ((TokenBalanceSortedItem) newItem).value.getFullName().equals(value.getFullName());
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
