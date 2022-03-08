package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.alphawallet.app.R;

public class SelectableItem extends LinearLayout {

    private View layout;
    private ImageView imageView;
    private TextView textView;
    private ImageView checkbox;

    public SelectableItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        layout = inflate(context, R.layout.item_selectable, this);

        imageView = findViewById(R.id.image);
        textView = findViewById(R.id.text);
        checkbox = findViewById(R.id.checkbox);
    }

    public void setup(@DrawableRes int image, @StringRes int text, boolean isChecked) {
        imageView.setImageResource(image);
        textView.setText(getContext().getString(text));
        setSelected(isChecked);
    }

    @Override
    public void setSelected(boolean selected) {
        checkbox.setSelected(selected);
    }

    @Override
    public boolean isSelected() {
        return checkbox.isSelected();
    }

    public String getText() {
        return textView.getText().toString();
    }
}
