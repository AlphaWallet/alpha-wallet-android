package com.alphawallet.app.ui.widget.entity;

import android.util.Pair;

import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.ui.widget.holder.NFTAssetHolder;

import java.math.BigInteger;

/**
 * Created by JB on 19/08/2021.
 */
public class NFTSortedItem extends SortedItem<Pair<BigInteger, NFTAsset>>
{
    public NFTSortedItem(Pair<BigInteger, NFTAsset> value, int weight) {
        super(NFTAssetHolder.VIEW_TYPE, value, new TokenPosition(TokenGroup.NFT, 1, weight));
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem)
    {
        return false;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        if (other instanceof NFTSortedItem)
        {
            NFTSortedItem otherItem = (NFTSortedItem) other;
            return other.viewType == NFTAssetHolder.VIEW_TYPE && this.viewType == NFTAssetHolder.VIEW_TYPE
                    && (value.second.equals(otherItem.value.second));
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean isRadioExposed()
    {
        return value.second.exposeRadio;
    }

    @Override
    public boolean isItemChecked()
    {
        return value.second.isChecked;
    }

    @Override
    public void setIsChecked(boolean b) { value.second.isChecked = b; }

    @Override
    public void setExposeRadio(boolean expose) { value.second.exposeRadio = expose; }
}
