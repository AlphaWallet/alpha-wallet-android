package com.alphawallet.app.ui.widget.entity;

import android.text.format.DateUtils;

import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by JB on 3/04/2020.
 */
public class EventSortedItem  extends TimestampSortedItem<EventMeta> {

    public EventSortedItem(int viewType, EventMeta value, int order) {
        super(viewType, value, 0, order);
    }

    @Override
    public int compare(SortedItem other)
    {
        if (other.tags.contains(IS_TIMESTAMP_TAG))
        {
            EventMeta oldTx;
            EventMeta newTx;
            TimestampSortedItem otherTimestamp = (TimestampSortedItem) other;
            //first see if this is a replacement TX
            if (viewType == TransactionHolder.VIEW_TYPE && viewType == other.viewType)
            {
                oldTx = value;
                newTx = (EventMeta) other.value;

                //In case two event have the same timestamp, need to disambiguate them to stop them hopping about
                if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
                {
                    return oldTx.hash.compareTo(newTx.hash);
                }
            }

            return super.compare(other);
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
                EventMeta oldTx = value;
                EventMeta newTx = (EventMeta) newItem.value;

                return oldTx.hash.equals(newTx.hash) && oldTx.eventDisplay.equals(newTx.eventDisplay);
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
                EventMeta oldTx = value;
                EventMeta newTx = (EventMeta) other.value;

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

