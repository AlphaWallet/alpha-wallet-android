package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.WalletConnectSessionHolder;

public class WalletConnectSessionSortedItem extends SortedItem
{
    public WalletConnectSessionSortedItem(int activeSessionsCount, int weight)
    {
        super(WalletConnectSessionHolder.VIEW_TYPE, activeSessionsCount, new TokenPosition(weight));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == viewType;
    }
}
