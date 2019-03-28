package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.ui.widget.holder.AssetInstanceScriptHolder;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceSortedItem extends SortedItem<TicketRange>
{
    public static final int VIEW_TYPE = AssetInstanceScriptHolder.VIEW_TYPE;

    public AssetInstanceSortedItem(TicketRange data, int weight) {
        super(VIEW_TYPE, data, weight);
    }

    @Override
    public int compare(SortedItem other)
    {
        return weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        return other.viewType == AssetInstanceScriptHolder.VIEW_TYPE && this.viewType == AssetInstanceScriptHolder.VIEW_TYPE
                && ( value.equals(other.value));
    }
}