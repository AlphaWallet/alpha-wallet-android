package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.util.LocaleUtils;

import java.util.Date;

public class TransactionDateHolder extends BinderViewHolder<Date> {

    public static final int VIEW_TYPE = 1004;
    private final TextView title;
    private final View separator;

    public TransactionDateHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        title = findViewById(R.id.text_header);
        separator = findViewById(R.id.separator);
    }

    @Override
    public void bind(@Nullable Date data, @NonNull Bundle addition) {
        if (data == null) {
            title.setText(null);
        } else {
            title.setText(getDate(data));
        }
        separator.setVisibility(View.GONE);
    }

    private String getDate(Date date)
    {
        if (DateUtils.isToday(date.getTime()))
        {
            return getString(R.string.today);
        }
        else
        {
            java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, LocaleUtils.getDeviceLocale(getContext()));
            return dateFormat.format(date);
        }
    }
}
