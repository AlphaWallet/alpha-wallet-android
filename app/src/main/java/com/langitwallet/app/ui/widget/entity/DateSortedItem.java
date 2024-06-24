package com.langitwallet.app.ui.widget.entity;

import com.langitwallet.app.ui.widget.holder.TransactionDateHolder;
import com.langitwallet.app.util.LocaleUtils;

import java.util.Date;

public class DateSortedItem extends TimestampSortedItem<Date> {
    public DateSortedItem(Date value) {
        super(TransactionDateHolder.VIEW_TYPE, value, 0, DESC);
    }

    @Override
    public Date getTimestamp() {
        return value;
    }

    @Override
    public boolean areContentsTheSame(SortedItem newItem) {
        return viewType == newItem.viewType && value.equals(((TimestampSortedItem) newItem).value);
    }

    @Override
    public boolean areItemsTheSame(SortedItem other) {
        return viewType == other.viewType;
    }

    public static DateSortedItem round(long timeStampInSec) {
        return new DateSortedItem(LocaleUtils.getLocalDateFromTimestamp(timeStampInSec));
    }

    public long getUID()
    {
        return ((Date)value).getTime();
    }
}
