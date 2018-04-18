package io.awallet.crypto.alphawallet.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.awallet.crypto.alphawallet.R;

public class InputView extends LinearLayout {
    private TextView label;
    private TextView error;
    private EditText editText;
    private int labelResId;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try {
            labelResId = a.getResourceId(R.styleable.InputView_label, R.string.empty);
        } finally {
            a.recycle();
        }

        inflate(context, R.layout.layout_alpha_input, this);

        label = findViewById(R.id.label);
        label.setText(labelResId);
        error = findViewById(R.id.error);
        editText = findViewById(R.id.edit_text);
    }

    public CharSequence getText() {
        return this.editText.getText();
    }

    public void setText(CharSequence text) {
        this.editText.setText(text);
    }

    public void setError(int resId) {
        if (resId == R.string.empty) {
            error.setText(resId);
            error.setVisibility(View.GONE);
        } else {
            error.setText(resId);
            error.setVisibility(View.VISIBLE);
        }
    }

    public void setError(CharSequence message) {
        if (message.toString().isEmpty()) {
            error.setText(message);
            error.setVisibility(View.GONE);
        } else {
            error.setText(message);
            error.setVisibility(View.VISIBLE);
        }
    }
}
