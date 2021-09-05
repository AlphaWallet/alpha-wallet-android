package com.alphawallet.app.widget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spanned;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;

public class TokenInfoView extends LinearLayout {
    private final TextView label;
    private final TextView value;
    private final TextView valueLongText;
    private boolean isLink;

    public TokenInfoView(Context context, String labelText)
    {
        super(context);
        inflate(context, R.layout.item_token_info, this);
        label = findViewById(R.id.label);
        value = findViewById(R.id.value);
        valueLongText = findViewById(R.id.value_long);

        label.setText(labelText);
        isLink = false;
    }

    public void setLabel(String text)
    {
        label.setText(text);
    }

    public void setValue(Spanned text)
    {
        TextView useView = getTextView(text.length());
        useView.setText(text);
    }

    public void setValue(String text)
    {
        if (text.startsWith("http")) { setLink(); }
        TextView useView = getTextView(text.length());
        useView.setText(text);
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
        value.setTextColor(ContextCompat.getColor(getContext(), R.color.azure));
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
}
