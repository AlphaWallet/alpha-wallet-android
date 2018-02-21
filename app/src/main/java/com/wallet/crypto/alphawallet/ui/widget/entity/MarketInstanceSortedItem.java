package com.wallet.crypto.alphawallet.ui.widget.entity;

import com.wallet.crypto.alphawallet.entity.MarketInstance;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.ui.widget.holder.MarketOrderHeaderHolder;
import com.wallet.crypto.alphawallet.ui.widget.holder.OrderHolder;

/**
 * Created by James on 21/02/2018.
 */

public class MarketInstanceSortedItem extends SortedItem<MarketInstance>
{
    public MarketInstanceSortedItem(MarketInstance value, int weight)
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
                || (((MarketInstanceSortedItem) newItem).value.contractAddress == value.contractAddress)
                && ((MarketInstanceSortedItem) newItem).value.signature.hashCode() == value.signature.hashCode();
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
