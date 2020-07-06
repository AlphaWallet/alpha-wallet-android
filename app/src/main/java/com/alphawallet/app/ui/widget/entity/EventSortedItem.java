package com.alphawallet.app.ui.widget.entity;

import android.text.format.DateUtils;

import com.alphawallet.app.entity.EventMeta;

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
            //first see if this is a replacement TX
            /*if (viewType == EventHolder.VIEW_TYPE && viewType == other.viewType)
            {
                TransactionMeta oldTx = (TransactionMeta) value;
                TransactionMeta newTx = (TransactionMeta) other.value;

                // Check if this is a written block replacing a pending block
                if (oldTx.hash.equals(newTx.hash)) return 0; // match

                //we were getting an instance where two transactions went through on the same
                //block - so the timestamp was the same. The display flickered between the two transactions.
                if (this.getTimestamp().equals(otherTimestamp.getTimestamp()))
                {
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
            }*/
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
                EventMeta oldEv = value;
                EventMeta newEv = (EventMeta) newItem.value;

                boolean timeStampMatch = oldEv.timeStamp == newEv.timeStamp;
                boolean messageMatch = (oldEv.eventMessage != null && newEv.eventMessage != null && oldEv.eventMessage.equals(newEv.eventMessage));
                boolean contractMatch = oldEv.tokenAddress.address.equalsIgnoreCase(newEv.tokenAddress.address) && oldEv.tokenAddress.chainId == newEv.tokenAddress.chainId;

                return timeStampMatch && messageMatch && contractMatch;
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
            /*if (viewType == other.viewType && viewType == EventHolder.VIEW_TYPE)
            {
                if (value instanceof TransactionMeta && other.value instanceof TransactionMeta)
                {
                    return ((TransactionMeta)value).hash.equals(((TransactionMeta)other.value).hash);
                }
                else if (value instanceof EventMeta && other.value instanceof EventMeta)
                {
                    return value.equals(other.value);
                }
                else
                {
                    return false;
                }
            }
            else*/
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
