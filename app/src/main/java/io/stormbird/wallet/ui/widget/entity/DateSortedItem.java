package io.stormbird.wallet.ui.widget.entity;

import android.text.format.DateUtils;

import io.stormbird.wallet.ui.widget.holder.TransactionDateHolder;
import io.stormbird.wallet.util.LocaleUtils;
import io.stormbird.wallet.util.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
}
