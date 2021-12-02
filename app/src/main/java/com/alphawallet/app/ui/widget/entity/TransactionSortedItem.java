package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.ui.widget.holder.EventHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;
import com.alphawallet.app.ui.widget.holder.TransferHolder;

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

            if (other.value instanceof ActivityMeta)
            {
                ActivityMeta otherMeta = (ActivityMeta) other.value;
                if (other.viewType == TransactionHolder.VIEW_TYPE && otherMeta.hash.equals(value.hash) //the pending tx time will be different from the written tx time
                        && value.isPending != ((TransactionMeta)otherMeta).isPending)
                {
                    return 0; //if comparing the same tx hash with a different time, they are the same
                }

                // Check if this is a written block replacing a pending block
                if (value.hash.equals(otherMeta.hash) && otherMeta.getTimeStamp() == value.getTimeStamp()) return 0; // match

                //we were getting an instance where two transactions went through on the same
                //block - so the timestamp was the same. The display flickered between the two transactions.
                if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
                {
                    return value.hash.compareTo(otherMeta.hash);
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
        else
        {
            return super.compare(other);
        }
    }

    @Override
    public boolean areContentsTheSame(SortedItem other) {
        try
        {
            if (viewType == other.viewType)
            {
                TransactionMeta newTx = (TransactionMeta) other.value;
                return value.isPending == newTx.isPending;
            }
            else //allow other types to override the lowly Transaction item
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
            //allow both Event and Transfer to override
            if (viewType == other.viewType)
            {
                TransactionMeta oldTx = (TransactionMeta) other.value;
                return value.hash.equals(oldTx.hash);
            }
            else return other.viewType == EventHolder.VIEW_TYPE || other.viewType == TransferHolder.VIEW_TYPE;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public Date getTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(value.getTimeStamp());
        return calendar.getTime();
    }
}
