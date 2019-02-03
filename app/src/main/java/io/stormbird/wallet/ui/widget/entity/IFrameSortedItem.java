package io.stormbird.wallet.ui.widget.entity;

import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.ui.widget.holder.IFrameHolder;

/**
 * Created by James on 14/12/2018.
 * Stormbird in Singapore
 */
public class IFrameSortedItem extends SortedItem<TicketRange>
{
    public static final int VIEW_TYPE = IFrameHolder.VIEW_TYPE;

    public IFrameSortedItem(TicketRange range, int weight) {
        super(VIEW_TYPE, range, weight);
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
        return other.viewType == IFrameHolder.VIEW_TYPE && this.viewType == IFrameHolder.VIEW_TYPE
                && ( ((IFrameSortedItem) other).value.tokenIds.size() == value.tokenIds.size()
                && ((IFrameSortedItem) other).value.tokenIds.get(0).compareTo(value.tokenIds.get(0)) == 0);
    }
}
