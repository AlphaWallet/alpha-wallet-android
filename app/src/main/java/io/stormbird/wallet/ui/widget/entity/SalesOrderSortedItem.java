package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.wallet.ui.widget.holder.OrderHolder;
import io.stormbird.token.entity.MagicLinkData;

/**
 * Created by James on 21/02/2018.
 */

public class SalesOrderSortedItem extends SortedItem<MagicLinkData>
{
    public SalesOrderSortedItem(MagicLinkData value, int weight)
    {
        super(OrderHolder.VIEW_TYPE, value, weight);
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
                || (((SalesOrderSortedItem) newItem).value.contractAddress == value.contractAddress)
                && ((SalesOrderSortedItem) newItem).value.signature.hashCode() == value.signature.hashCode();
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
