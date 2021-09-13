package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;

public class LargeTitleView extends LinearLayout {

    public final TextView title;
    public final TextView subtitle;

    public LargeTitleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.layout_large_title_view, this, true);

        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
    }
}
