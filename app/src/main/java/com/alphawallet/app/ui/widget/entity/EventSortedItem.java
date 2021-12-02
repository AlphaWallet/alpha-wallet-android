package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;
import com.alphawallet.app.ui.widget.holder.TransferHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by JB on 7/07/2020.
 */
public class EventSortedItem extends TimestampSortedItem<EventMeta>
{
    public EventSortedItem(int viewType, EventMeta value, int order) {
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
                // Check if this is a written block replacing a pending block
                if (value.hash.equals(otherMeta.hash) && otherMeta.getTimeStamp() == value.getTimeStamp()) return 0; // match

                //we were getting an instance where two transactions went through on the same
                //block - so the timestamp was the same. The display flickered between the two transactions.
                if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
                {
                    if (other.viewType == TransactionHolder.VIEW_TYPE) return 0;
                    if (other.viewType == TransferHolder.VIEW_TYPE) return 0;
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
    public boolean areContentsTheSame(SortedItem other)
    {
        if (viewType == other.viewType)
        {
            EventMeta oldTx = value;
            EventMeta newTx = (EventMeta) other.value;
            return oldTx.hash.equals(newTx.hash) && oldTx.activityCardName.equals(newTx.activityCardName);
        }
        else if (other.viewType == TransactionHolder.VIEW_TYPE)
        {
            return true;
        }
        else return other.viewType == TransferHolder.VIEW_TYPE;
    }

    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        if (viewType == other.viewType)
        {
            EventMeta oldTx = value;
            EventMeta newTx = (EventMeta) other.value;

            return oldTx.hash.equals(newTx.hash) && oldTx.activityCardName.equals(newTx.activityCardName);
        }
        else if (other.viewType == TransactionHolder.VIEW_TYPE)
        {
            return true;
        }
        else return other.viewType == TransferHolder.VIEW_TYPE;
    }

    @Override
    public Date getTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(value.getTimeStamp());
        return calendar.getTime();
    }
}
