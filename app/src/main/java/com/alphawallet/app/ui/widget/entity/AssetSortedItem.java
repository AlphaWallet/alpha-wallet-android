package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.OpenseaHolder;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class AssetSortedItem extends SortedItem<TicketRange>
{
    public AssetSortedItem(TicketRange value, int weight) {
        super(OpenseaHolder.VIEW_TYPE, value, weight);
    }

    @Override
    public int compare(SortedItem other) {
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
        return other.viewType == OpenseaHolder.VIEW_TYPE && this.viewType == OpenseaHolder.VIEW_TYPE
                && ( value.equals(other.value));
    }

    @Override
    public boolean isRadioExposed()
    {
        return value.exposeRadio;
    }

    @Override
    public boolean isItemChecked()
    {
        return value.isChecked;
    }

    @Override
    public void setIsChecked(boolean b) { value.isChecked = b; };

    @Override
    public void setExposeRadio(boolean expose) { value.exposeRadio = expose; };

    @Override
    public List<BigInteger> getTokenIds()
    {
        return value.tokenIds;
    }
}