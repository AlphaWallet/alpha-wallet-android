package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.util.Utils;

import com.alphawallet.app.R;

public class InputView extends LinearLayout {
    private Context context;

    private TextView label;
    private TextView error;
    private EditText editText;
    private CheckBox togglePassword;

    private int labelResId;
    private int lines;
    private String inputType;
    private int buttonSrc;
    private String imeOptions;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        getAttrs(context, attrs);

        bindViews();

        setLines();

        setImeOptions();

        setInputType();
    }

    private void bindViews() {
        inflate(context, R.layout.layout_input_view, this);

        label = findViewById(R.id.label);
        label.setText(labelResId);
        error = findViewById(R.id.error);
        editText = findViewById(R.id.edit_text);
        togglePassword = findViewById(R.id.toggle_password);
        togglePassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            editText.setSelection(editText.getText().length());
        });
    }

    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try {
            labelResId = a.getResourceId(R.styleable.InputView_label, R.string.empty);
            lines = a.getInt(R.styleable.InputView_lines, 1);
            inputType = a.getString(R.styleable.InputView_inputType);
            imeOptions = a.getString(R.styleable.InputView_imeOptions);
        } finally {
            a.recycle();
        }
    }

    private void setInputType() {
        if (inputType != null) {
            switch (inputType) {
                case "textPassword": {
                    editText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                    togglePassword.setVisibility(View.VISIBLE);
                    editText.setPadding(
                            Utils.dp2px(context, 15),
                            Utils.dp2px(context, 5),
                            Utils.dp2px(context, 50),
                            Utils.dp2px(context, 5)
                    );
                    editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    break;
                }
                case "number": {
                    editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                    break;
                }
            }
        }
    }

    private void setImeOptions() {
        if (imeOptions != null) {
            switch (imeOptions) {
                case "actionNext": {
                    editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                    break;
                }
                case "actionDone": {
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    break;
                }
            }
        }
    }

    private void setLines() {
        if (lines > 1) {
            editText.setGravity(Gravity.TOP);
            editText.setPadding(
                    Utils.dp2px(context, 15),
                    Utils.dp2px(context, 10),
                    Utils.dp2px(context, 15),
                    Utils.dp2px(context, 10)
            );
        }

        editText.setLines(lines);
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
        if (message == null) {
            error.setVisibility(View.GONE);
        } else if (message.toString().isEmpty()) {
            error.setText(message);
            error.setVisibility(View.GONE);
        } else {
            error.setText(message);
            error.setVisibility(View.VISIBLE);
        }
    }
}
