package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.wallet.entity.OpenseaElement;
import io.stormbird.wallet.ui.widget.holder.OpenseaHolder;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaSortedItem extends SortedItem<OpenseaElement>
{
    public OpenseaSortedItem(OpenseaElement value, int weight) {
        super(OpenseaHolder.VIEW_TYPE, value, weight);
    }

    @Override
    public int compare(SortedItem other) {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return (newItem.viewType == viewType
                && ((OpenseaSortedItem) newItem).value.tokenId == value.tokenId);
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return (other.viewType == viewType
                && ((OpenseaSortedItem) other).value.tokenId == value.tokenId);
    }
}