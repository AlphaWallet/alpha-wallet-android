package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.WalletConnectSessionHolder;

public class WalletConnectSessionSortedItem extends SortedItem<Integer>
{
    public WalletConnectSessionSortedItem(int weight)
    {
        super(WalletConnectSessionHolder.VIEW_TYPE, 0, new TokenPosition(weight));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return true;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
