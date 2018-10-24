package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.ui.widget.holder.OpenseaHolder;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class AssetSortedItem extends SortedItem<Asset>
{
    public AssetSortedItem(Asset value, int weight) {
        super(OpenseaHolder.VIEW_TYPE, value, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return (newItem.viewType == viewType
                && ((AssetSortedItem) newItem).value.getTokenId().equals(value.getTokenId()));
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return (other.viewType == viewType
                && ((AssetSortedItem) other).value.getTokenId().equals(value.getTokenId()));
    }
}