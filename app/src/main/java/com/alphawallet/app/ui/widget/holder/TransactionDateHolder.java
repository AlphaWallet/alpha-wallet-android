package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.R;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TransactionDateHolder extends BinderViewHolder<Date> {

    public static final int VIEW_TYPE = 1004;
    private final TextView title;

    public TransactionDateHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        title = findViewById(R.id.title);
    }

    @Override
    public void bind(@Nullable Date data, @NonNull Bundle addition) {
        if (data == null) {
            title.setText(null);
        } else {
            title.setText(getDate(data));
        }
    }

    private String getDate(Date date) {
        Locale               locale     = Locale.getDefault();
        java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale);
        return dateFormat.format(date);
    }
}
