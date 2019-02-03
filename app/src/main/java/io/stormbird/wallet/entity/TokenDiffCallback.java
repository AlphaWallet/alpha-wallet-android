package io.stormbird.wallet.entity;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.SortedList;

import io.stormbird.wallet.ui.widget.entity.SortedItem;
import io.stormbird.wallet.ui.widget.entity.TotalBalanceSortedItem;

import java.math.BigDecimal;

/**
 * Created by James on 15/03/2018.
 */

public class TokenDiffCallback extends DiffUtil.Callback {

    private final SortedList<SortedItem> mOldTokenList;
    private final SortedList<SortedItem> mNewTokenList;

    public TokenDiffCallback(SortedList<SortedItem> oldList, SortedList<SortedItem> newList) {
        this.mOldTokenList = oldList;
        this.mNewTokenList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldTokenList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewTokenList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Object oldItem = mOldTokenList.get(oldItemPosition).value;
        Object newItem = mNewTokenList.get(newItemPosition).value;

        if (oldItem == null || newItem == null) return false;

        if (oldItem.getClass() != newItem.getClass())
        {
            return false;
        }
        else if (oldItem instanceof TotalBalanceSortedItem)
        {
            //did balance change?
            return ((TotalBalanceSortedItem) oldItem).areItemsTheSame((TotalBalanceSortedItem)newItem);
        }
        else if (oldItem instanceof BigDecimal)
        {
            return true; // both must be BigDecimal
        }
        else
        {
            //they're both tokens, check if address is the same
            Token oldToken = (Token)oldItem;
            Token newToken = (Token)newItem;

            return oldToken.getAddress().equalsIgnoreCase(newToken.getAddress());
        }
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
    {
        Object oldItem = mOldTokenList.get(oldItemPosition).value;
        Object newItem = mNewTokenList.get(newItemPosition).value;

        if (oldItem == null || newItem == null) return false;

        if (oldItem.getClass() != newItem.getClass())
        {
            return false;
        }
        else if (oldItem instanceof TotalBalanceSortedItem)
        {
            //did balance change?
            return ((TotalBalanceSortedItem) oldItem).areContentsTheSame((TotalBalanceSortedItem)newItem);
        }
        else if (oldItem instanceof BigDecimal)
        {
            return ((BigDecimal) oldItem).compareTo((BigDecimal) newItem) == 0;
        }
        else
        {
            //they're both tokens
            Token oldToken = (Token)oldItem;
            Token newToken = (Token)newItem;

            if (!oldToken.getAddress().equalsIgnoreCase(newToken.getAddress()))
            {
                return false;
            }
            else if (!oldToken.getStringBalance().equals(newToken.getStringBalance()))
            {
                return false;
            }
            else
                return oldToken.ticker == null || newToken.ticker == null || oldToken.ticker.percentChange24h.equals(newToken.ticker.percentChange24h);
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}