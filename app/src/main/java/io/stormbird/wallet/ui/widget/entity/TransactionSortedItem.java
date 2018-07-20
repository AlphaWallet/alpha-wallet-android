package io.stormbird.wallet.ui.widget.entity;

import android.text.format.DateUtils;

import io.stormbird.wallet.entity.Transaction;
import io.stormbird.wallet.entity.TransactionContract;
import io.stormbird.wallet.ui.widget.holder.TransactionHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TransactionSortedItem extends TimestampSortedItem<Transaction> {

    public TransactionSortedItem(int viewType, Transaction value, int order) {
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
                Transaction oldTx = value;
                Transaction newTx = (Transaction) other.value;

                // Note: This fails if the transaction is badly formed - this is intentional as we need to be certain that
                //       All failure mechanisms have been removed
                return oldTx.contentHash.compareTo(newTx.contentHash);
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
                Transaction oldTx = value;
                Transaction newTx = (Transaction) newItem.value;

                //return oldTx.contentHash.equals(newTx.contentHash);
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
                Transaction oldTx = value;
                Transaction newTx = (Transaction) other.value;

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
