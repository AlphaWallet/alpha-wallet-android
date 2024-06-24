package com.langitwallet.app.ui.widget.entity;

import com.alphawallet.token.entity.TicketRange;
import com.langitwallet.app.ui.widget.holder.AssetInstanceScriptHolder;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceSortedItem extends SortedItem<TicketRange>
{
    public static final int VIEW_TYPE = AssetInstanceScriptHolder.VIEW_TYPE;

    public AssetInstanceSortedItem(TicketRange data, TokenPosition weight) {
        super(VIEW_TYPE, data, weight);
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
