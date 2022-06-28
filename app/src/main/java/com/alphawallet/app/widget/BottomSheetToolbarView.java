package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.bumptech.glide.Glide;

public class BottomSheetToolbarView extends RelativeLayout
{
    private TextView title;
    private ImageView logo;
    private ImageView closeBtn;

    public BottomSheetToolbarView(Context ctx, @Nullable AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.layout_bottom_sheet_toolbar, this);
        title = findViewById(R.id.title);
        logo = findViewById(R.id.logo);
        closeBtn = findViewById(R.id.image_close);

        getAttrs(ctx, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BottomSheetToolbarView,
                0, 0
        );

        try
        {
            int titleRes = a.getResourceId(R.styleable.BottomSheetToolbarView_title, R.string.empty);
            title.setText(titleRes);
        }
        finally
        {
            a.recycle();
        }
    }

    public void setTitle(int titleRes)
    {
        title.setText(titleRes);
    }

    public void setTitle(CharSequence titleText)
    {
        title.setText(titleText);
    }

    public void setLogo(Context context, String imageUrl)
    {
        Glide.with(context)
                .load(imageUrl)
                .circleCrop()
                .into(logo);
    }

    public void setCloseListener(View.OnClickListener listener)
    {
        closeBtn.setOnClickListener(listener);
    }
}
