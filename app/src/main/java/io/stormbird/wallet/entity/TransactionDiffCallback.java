package io.stormbird.wallet.entity;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.SortedList;

import io.stormbird.wallet.ui.widget.entity.SortedItem;

import java.util.Date;

/**
 * Created by James on 15/03/2018.
 */

public class TransactionDiffCallback extends DiffUtil.Callback {

    private final SortedList<SortedItem> mOldList;
    private final SortedList<SortedItem> mNewList;

    public TransactionDiffCallback(SortedList<SortedItem> oldList, SortedList<SortedItem> newList) {
        this.mOldList = oldList;
        this.mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Object oldItem = mOldList.get(oldItemPosition).value;
        Object newItem = mNewList.get(newItemPosition).value;

        if (oldItem == null || newItem == null) return false;

        //first spot type mismatch
        if (oldItem.getClass() != newItem.getClass()) return false;

        if (oldItem instanceof Date)
        {
            Date oldDate = (Date)oldItem;
            Date newDate = (Date)oldItem;
            return oldDate.equals(newDate);
        }
        else
        {
            Transaction oldTx = (Transaction)oldItem;
            Transaction newTx = (Transaction)newItem;
            return (oldTx.hash.equals(newTx.hash));
        }
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Object oldItem = mOldList.get(oldItemPosition).value;
        Object newItem = mNewList.get(newItemPosition).value;

        if (oldItem == null || newItem == null) return false;

        //first spot type mismatch
        if (oldItem.getClass() != newItem.getClass()) return false;

        if (oldItem instanceof Date)
        {
            Date oldDate = (Date)oldItem;
            Date newDate = (Date)oldItem;
            return oldDate.equals(newDate);
        }
        else
        {
            Transaction oldTx = (Transaction)oldItem;
            Transaction newTx = (Transaction)newItem;
            //check
            if (oldTx.operations == null && newTx.operations != null) return false;
            if (oldTx.operations != null && oldTx.operations.length == 0 && newTx.operations != null && newTx.operations.length > 0) return false;

            if (oldTx.operations.length == 1 && newTx.operations.length == 1)
            {
                TransactionContract oldTc = oldTx.operations[0].contract;
                TransactionContract newTc = newTx.operations[0].contract;

                if (oldTc.getClass() != newTc.getClass()) return false;

                return oldTc.name != null || newTc.name == null;
            }
            //
            //if (newTx.operations == null || newTx.operations.length == 0 || newTx.operations[0].contract == null) return true;



            return true;// must be the same
        }
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}