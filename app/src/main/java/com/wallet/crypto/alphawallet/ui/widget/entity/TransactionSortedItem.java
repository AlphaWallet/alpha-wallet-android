package com.wallet.crypto.alphawallet.ui.widget.entity;

import android.text.format.DateUtils;

import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.TransactionContract;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TransactionSortedItem extends TimestampSortedItem<Transaction> {

    public TransactionSortedItem(int viewType, Transaction value, int order) {
        super(viewType, value, 0, order);
    }

    @Override
    public int compare(SortedItem other) {
        return super.compare(other);
//        return other.viewType == TransactionHolder.VIEW_TYPE ||
//                ? super.compare(other)
//                : weight - other.weight;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        if (viewType == newItem.viewType) {
            Transaction oldTx = (Transaction) value;
            Transaction newTx = (Transaction) newItem.value;

            if (!oldTx.hash.equals(newTx.hash) || !(oldTx.timeStamp == newTx.timeStamp)) return false; //hash or timestamp mismatch

            //check operations
            if (oldTx.operations == null && newTx.operations != null) return false;
            if (oldTx.operations != null && oldTx.operations.length == 0 && newTx.operations != null && newTx.operations.length > 0)
                return false;

            if (oldTx.operations.length == 1 && newTx.operations.length == 1) {
                TransactionContract oldTc = oldTx.operations[0].contract;
                TransactionContract newTc = newTx.operations[0].contract;

                if (oldTc.getClass() != newTc.getClass()) return false;

                if (oldTc.name == null && newTc.name != null) return false;
            }

            return true;// must be the same
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return viewType == other.viewType;
    }

    @Override
    public Date getTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(value.timeStamp * DateUtils.SECOND_IN_MILLIS);
        return calendar.getTime();
    }
}
