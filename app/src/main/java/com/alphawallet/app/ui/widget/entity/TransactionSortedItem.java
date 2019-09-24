package com.alphawallet.app.ui.widget.entity;

import android.text.format.DateUtils;

import com.alphawallet.app.ui.widget.holder.TransactionHolder;
import com.alphawallet.app.entity.TransactionMeta;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TransactionSortedItem extends TimestampSortedItem<TransactionMeta> {

    public TransactionSortedItem(int viewType, TransactionMeta value, int order) {
        super(viewType, value, 0, order);
    }

    @Override
    public int compare(SortedItem other)
    {
        if (other.tags.contains(IS_TIMESTAMP_TAG))
        {
            TimestampSortedItem otherTimestamp = (TimestampSortedItem) other;
            //we were getting an instance where two transactions went through on the same
            //block - so the timestamp was the same. The display flickered between the two transactions.
            if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
            {
                TransactionMeta oldTx = value;
                TransactionMeta newTx = (TransactionMeta) other.value;

                //just compare the transaction hash instead
                return oldTx.hash.compareTo(newTx.hash);
            }
            else
            {
                return super.compare(other);
            }
        }
        else
        {
            return super.compare(other);
        }
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        try
        {
            if (viewType == newItem.viewType)
            {
                TransactionMeta oldTx = value;
                TransactionMeta newTx = (TransactionMeta) newItem.value;

                return oldTx.hash.equals(newTx.hash);
            }
            else
            {
                return false;
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        try
        {
            if (viewType == other.viewType && viewType == TransactionHolder.VIEW_TYPE)
            {
                TransactionMeta oldTx = value;
                TransactionMeta newTx = (TransactionMeta) other.value;

                return oldTx.hash.equals(newTx.hash);
            }
            else
            {
                return viewType == other.viewType;
            }
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public Date getTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(value.timeStamp * DateUtils.SECOND_IN_MILLIS);
        return calendar.getTime();
    }
}
