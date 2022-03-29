package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.alphawallet.app.R;

/**
 * Created by James on 20/07/2019.
 * Stormbird in Sydney
 */
public class TextHolder extends BinderViewHolder<String>
{
    public static final int VIEW_TYPE = 1041;

    private final TextView text;
    private final View separator;

    public TextHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        text = findViewById(R.id.text_header);
        separator = findViewById(R.id.separator);
    }

    @Override
    public void bind(@Nullable String data, @NonNull Bundle addition)
    {
        if (data != null && data.length() > 0)
        {
            text.setText(data);
            separator.setVisibility(View.GONE);
        }
    }
}
