package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.ui.widget.holder.OpenseaGridHolder;
import com.alphawallet.token.entity.TicketRange;

import java.math.BigInteger;
import java.util.List;


public class AssetGridSortedItem extends SortedItem<TicketRange>
{
    public AssetGridSortedItem(TicketRange value, int weight) {
        super(OpenseaGridHolder.VIEW_TYPE, value, weight);
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
        return other.viewType == OpenseaGridHolder.VIEW_TYPE && this.viewType == OpenseaGridHolder.VIEW_TYPE
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
    public void setIsChecked(boolean b) { value.isChecked = b; }

    @Override
    public void setExposeRadio(boolean expose) { value.exposeRadio = expose; }

    @Override
    public List<BigInteger> getTokenIds()
    {
        return value.tokenIds;
    }
}