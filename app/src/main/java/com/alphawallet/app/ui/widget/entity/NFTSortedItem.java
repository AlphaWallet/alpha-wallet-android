package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.ui.widget.holder.NFTAssetHolder;

/**
 * Created by JB on 19/08/2021.
 */
public class NFTSortedItem extends SortedItem<NFTAsset>
{
    public NFTSortedItem(NFTAsset value, int weight) {
        super(NFTAssetHolder.VIEW_TYPE, value, weight);
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
        return other.viewType == NFTAssetHolder.VIEW_TYPE && this.viewType == NFTAssetHolder.VIEW_TYPE
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
}
