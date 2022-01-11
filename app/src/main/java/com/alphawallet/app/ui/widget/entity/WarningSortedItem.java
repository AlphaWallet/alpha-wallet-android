package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.WarningHolder;

/**
 * Created by James on 18/07/2019.
 * Stormbird in Sydney
 */
public class WarningSortedItem extends SortedItem<WarningData> {

    public WarningSortedItem(WarningData value, int weight) {
        super(WarningHolder.VIEW_TYPE, value, new TokenPosition(weight));
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
