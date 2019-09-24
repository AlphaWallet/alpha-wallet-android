package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.MarketplaceEvent;

public class MarketplaceEventSortedItem extends SortedItem<MarketplaceEvent> {

    public MarketplaceEventSortedItem(int viewType, MarketplaceEvent value, int weight) {
        super(viewType, value, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        if (viewType == newItem.viewType) {
            MarketplaceEvent marketplaceEvent = (MarketplaceEvent) newItem.value;
            return value.getEventName().equals(marketplaceEvent.getEventName());
        }
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return viewType == other.viewType;
    }
}
