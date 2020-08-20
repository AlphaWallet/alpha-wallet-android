package com.alphawallet.app.ui.widget.entity;

import android.text.format.DateUtils;

import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.ui.widget.holder.EventHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;

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
            String otherHash = null;
            if (other.viewType == TransactionHolder.VIEW_TYPE)
            {
                otherHash = ((TransactionMeta) other.value).hash;
            }
            else if (other.viewType == EventHolder.VIEW_TYPE)
            {
                otherHash = ((EventMeta) other.value).hash;
            }

            if (otherHash != null)
            {
                // Check if this is a written block replacing a pending block
                if (value.hash.equals(otherHash)) return 0; // match

                //we were getting an instance where two transactions went through on the same
                //block - so the timestamp was the same. The display flickered between the two transactions.
                if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
                {
                    if (other.viewType == TransactionHolder.VIEW_TYPE) return 0;
                    EventMeta oldTx = value;
                    EventMeta newTx = (EventMeta) other.value;
                    if (oldTx.hash.equals(newTx.hash)) return oldTx.activityCardName.compareTo(newTx.activityCardName);
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
        else
        {
            return super.compare(other);
        }

        /*if (other.viewType == TransactionHolder.VIEW_TYPE && ((TransactionSortedItem)other).value.hash.equals(value.hash))
        {
            return 0;
        }
        else if (!other.tags.contains(IS_TIMESTAMP_TAG) || other.viewType != EventHolder.VIEW_TYPE)
        {
            return super.compare(other);
        }
        EventMeta oldTx = value;
        EventMeta newTx = (EventMeta) other.value;
        TimestampSortedItem otherTimestamp = (TimestampSortedItem) other;

        if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
        {
            if (oldTx.hash.equals(newTx.hash)) return oldTx.activityCardName.compareTo(newTx.activityCardName);
            return oldTx.hash.compareTo(newTx.hash);
        }
        else
        {
            return super.compare(other);
        }*/
    }

    /*@Override
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
            return false;
        }
        else
        {
            return false;
        }
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
            return false;
        }
        else
        {
            return false;
        }
    }*/

    @Override
    public boolean areContentsTheSame(SortedItem other)
    {
        if (viewType == other.viewType)
        {
            EventMeta oldTx = value;
            EventMeta newTx = (EventMeta) other.value;
            return oldTx.hash.equals(newTx.hash) && oldTx.activityCardName.equals(newTx.activityCardName);
        }
        else if (other.viewType == TransactionHolder.VIEW_TYPE)// && ((TransactionSortedItem)other).value.hash.equals(value.hash))
        {
            return true;
        }
        else
        {
            return false;
        }
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
        else if (other.viewType == TransactionHolder.VIEW_TYPE)// && ((TransactionSortedItem)other).value.hash.equals(value.hash))
        {
            return true;
        }
        else
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
