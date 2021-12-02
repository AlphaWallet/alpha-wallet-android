package com.alphawallet.app.ui.widget.entity;

import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.ui.widget.holder.EventHolder;
import com.alphawallet.app.ui.widget.holder.TransactionHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by JB on 18/12/2020.
 */
public class TransferSortedItem extends TimestampSortedItem<TokenTransferData> {

    public TransferSortedItem(int viewType, TokenTransferData value, int order) {
        super(viewType, value, 0, order);
    }

    @Override
    public int compare(SortedItem other)
    {
        if (other.tags.contains(IS_TIMESTAMP_TAG))
        {
            TimestampSortedItem<?> otherTimestamp = (TimestampSortedItem<?>) other;

            if (other.value instanceof ActivityMeta)
            {
                ActivityMeta otherMeta = (ActivityMeta) other.value;
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

    //Determine if we overwrite or add an extra element
    @Override
    public boolean areContentsTheSame(SortedItem other)
    {
        //allow event type to overwrite, messaging the adapter
        if (viewType == other.viewType)
        {
            return true; //don't overwrite
        }
        else return other.viewType != EventHolder.VIEW_TYPE;
    }

    //Checks if the type is the same, if same type then overwrite is possible
    @Override
    public boolean areItemsTheSame(SortedItem other)
    {
        //allow Event type to overwrite
        if (viewType == other.viewType)
        {
            TokenTransferData newTx = (TokenTransferData) other.value;
            return value.hash.equals(newTx.hash); //if same type, only overwrite if hash is same
        }
        else return other.viewType == EventHolder.VIEW_TYPE;
    }

    @Override
    public Date getTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(value.getTimeStamp());
        return calendar.getTime();
    }

    public long getUID()
    {
        return UUID.nameUUIDFromBytes((value.hash + value.getTimeStamp() + value.transferDetail).getBytes()).getMostSignificantBits();
    }
}
