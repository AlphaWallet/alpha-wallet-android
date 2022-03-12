package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;

import org.w3c.dom.Text;

import javax.annotation.Nullable;

public class DialogInfoItem extends LinearLayout {

    private final TextView label;
    private final TextView message;
    private final TextView actionText;

    public DialogInfoItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.item_dialog_info, this);
        label = findViewById(R.id.text_label);
        message = findViewById(R.id.text_message);
        actionText = findViewById(R.id.text_action);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.DialogInfoItem,
                0, 0);

        boolean showAction = a.getBoolean(R.styleable.DialogInfoItem_showActionText, false);
        setLabel(a.getString(R.styleable.DialogInfoItem_title));
        setMessage(a.getString(R.styleable.DialogInfoItem_text));
        actionText.setVisibility( showAction ? VISIBLE : INVISIBLE);
    }

    public void setLabel(String label) {
        this.label.setText(label);
    }

    public void setMessage(String msg) {
        this.message.setText(msg);
    }

    public void setMessageTextColor(@ColorRes int color) {
        this.message.setTextColor(ContextCompat.getColor(getContext(), color));
    }

    public void setActionText(String text) {
        this.actionText.setText(text);
    }

    public void setActionListener(View.OnClickListener listener) {
        actionText.setOnClickListener(listener);
    }
}
