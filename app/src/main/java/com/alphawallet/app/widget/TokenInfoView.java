package com.alphawallet.app.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.service.TickerService;

public class TokenInfoView extends LinearLayout
{
    private TextView label;
    private TextView value;
    private TextView valueLongText;
    private boolean isLink;
    private boolean hasPrefix = false;

    public TokenInfoView(Context context, String labelText)
    {
        this(context, (AttributeSet) null);
        label.setText(labelText);
        isLink = false;
    }

    public TokenInfoView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_token_info, this);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.TokenInfoView,
                0, 0
        );

        try
        {
            int labelRes = a.getResourceId(R.styleable.TokenInfoView_tokenInfoLabel, R.string.empty);
            label = findViewById(R.id.label);
            value = findViewById(R.id.value);
            valueLongText = findViewById(R.id.value_long);

            label.setText(labelRes);
        }
        finally
        {
            a.recycle();
        }
    }

    public void setLabel(String text)
    {
        label.setText(text);
    }

    public void setValue(String text)
    {
        if (!TextUtils.isEmpty(text))
        {
            setVisibility(View.VISIBLE);
            if (text.startsWith("http"))
            {
                setLink();
            }
            TextView useView = getTextView(text.length());
            useView.setText(text);
        }
    }

    public void setCurrencyValue(double v)
    {
        setVisibility(View.VISIBLE);
        value.setVisibility(View.VISIBLE);
        valueLongText.setVisibility(View.GONE);
        String prefix = hasPrefix && v > 0 ? "+" : "";
        value.setText(prefix + TickerService.getFullCurrencyString(v));

        int color = ContextCompat.getColor(getContext(), v < 0 ? R.color.negative : R.color.positive);
        value.setTextColor(color);
    }

    private TextView getTextView(int length)
    {
        if (length < 23 || isLink)
        {
            value.setVisibility(View.VISIBLE);
            valueLongText.setVisibility(View.GONE);
            return value;
        }
        else
        {
            value.setVisibility(View.GONE);
            valueLongText.setVisibility(View.VISIBLE);
            return valueLongText;
        }
    }

    public void setLink()
    {
        isLink = true;
        value.setTextColor(ContextCompat.getColor(getContext(), R.color.brand));
        value.setOnClickListener(v -> {
            String url = value.getText().toString();
            if (url.startsWith("http"))
            {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                getContext().startActivity(i);
            }
        });
    }

    public void setHasPrefix(boolean hasPrefix)
    {
        this.hasPrefix = hasPrefix;
    }
}
