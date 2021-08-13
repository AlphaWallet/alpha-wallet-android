package com.alphawallet.app.widget;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spanned;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;

public class TokenInfoView extends LinearLayout {
    private final TextView label;
    private final TextView value;

    public TokenInfoView(Context context, String labelText)
    {
        super(context);
        inflate(context, R.layout.item_token_info, this);
        label = findViewById(R.id.label);
        value = findViewById(R.id.value);

        label.setText(labelText);
    }

    public void setLabel(String text)
    {
        label.setText(text);
    }

    public void setValue(Spanned text)
    {
        value.setText(text);
    }

    public void setValue(String text)
    {
        value.setText(text);
    }

    public void setLink()
    {
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
