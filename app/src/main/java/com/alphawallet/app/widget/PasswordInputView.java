package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.util.Utils;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

public class PasswordInputView extends LinearLayout implements TextView.OnEditorActionListener
{
    private final Context context;

    private final TextView label;
    private final TextView error;
    private final TextView status;
    private final EditText editText;
    private final CheckBox togglePassword;
    private final TextView instruction;

    private int labelResId;
    private int lines;
    private String inputType;
    private int minHeight;
    private String imeOptions;
    private String hintTxt;
    private Activity activity;
    private LayoutCallbackListener callbackListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PasswordInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getAttrs(context, attrs);

        inflate(context, R.layout.layout_password_input, this);

        label = findViewById(R.id.label);
        error = findViewById(R.id.error);
        editText = findViewById(R.id.edit_text);
        status = findViewById(R.id.status_text);
        instruction = findViewById(R.id.instruction);
        togglePassword = findViewById(R.id.toggle_password);
        findViewById(R.id.text_word_count).setVisibility(View.GONE);

        setViews();
        setImeOptions();
        setInputType();
        setMinHeight();
        setLines();
    }

    public void setLayoutListener(Activity a, LayoutCallbackListener callback)
    {
        activity = a;
        callbackListener = callback;
        getEditText().setOnEditorActionListener(this);

        addKeyboardListener(callback);
    }

    public EditText getEditText()
    {
        return editText;
    }

    private void setViews()
    {
        label.setText(labelResId);
        if (labelResId != R.string.empty) label.setVisibility(View.VISIBLE);
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
            minHeight = a.getInteger(R.styleable.InputView_minHeightValue, 0);
            hintTxt = a.getString(R.styleable.InputView_hint);
        } finally {
            a.recycle();
        }
    }

    private void setMinHeight() {
        Resources r = getResources();
        if (minHeight > 0)
        {
            int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minHeight, r.getDisplayMetrics());
            editText.setMinHeight(px);
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
                case "textNoSuggestions":{
                    editText.setInputType(editText.getInputType() |
                            EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                            EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE |
                            EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    break;
                }
            }
        }
        editText.setTypeface(Typeface.DEFAULT);
        if (!TextUtils.isEmpty(hintTxt)) editText.setHint(hintTxt);
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
                    editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
                    break;
                }
            }
        }
    }

    private void setLines() {
        if (lines > 1) {
            editText.setGravity(Gravity.TOP);
            editText.setPadding(
                    Utils.dp2px(context, 20),
                    Utils.dp2px(context, 16),
                    Utils.dp2px(context, 20),
                    Utils.dp2px(context, 16)
            );
        }

        editText.setMinLines(lines);
    }

    public CharSequence getText() {
        return this.editText.getText();
    }

    public void setInstruction(int resourceId)
    {
        this.instruction.setText(resourceId);
    }

    public void setText(CharSequence text) {
        this.editText.setText(text);
    }

    public void setError(int resId) {
        if (resId == R.string.empty) {
            error.setText(resId);
            error.setVisibility(View.GONE);
            editText.setBackgroundResource(R.drawable.background_password_entry);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.silver));
        } else {
            error.setText(resId);
            error.setVisibility(View.VISIBLE);
            editText.setBackgroundResource(R.drawable.background_password_error);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.warning_red));
        }
    }

    public void setStatus(CharSequence statusTxt)
    {
        if (TextUtils.isEmpty(statusTxt)) {
            status.setVisibility(View.GONE);
        } else {
            status.setText(statusTxt);
            status.setVisibility(View.VISIBLE);
        }
    }

    public void setError(CharSequence message) {
        if (message == null) {
            error.setVisibility(View.GONE);
            editText.setBackgroundResource(R.drawable.background_password_entry);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.silver));
        } else if (message.toString().isEmpty()) {
            error.setText(message);
            error.setVisibility(View.GONE);
            editText.setBackgroundResource(R.drawable.background_password_entry);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.silver));
        } else {
            error.setText(message);
            error.setVisibility(View.VISIBLE);
            editText.setBackgroundResource(R.drawable.background_password_error);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.warning_red));
        }
    }

    public boolean isErrorState()
    {
        return error.getVisibility() == View.VISIBLE;
    }

    private void addKeyboardListener(LayoutCallbackListener callback)
    {
        KeyboardVisibilityEvent.setEventListener(
                activity, isOpen -> {
                    if (isOpen)
                    {
                        callback.onLayoutShrunk();
                    }
                    else
                    {
                        callback.onLayoutExpand();
                    }
                });
    }

    @Override
    public boolean onEditorAction(TextView view, int i, KeyEvent keyEvent)
    {
        if (isErrorState())
        {
            flashLayout();
        }
        else if (callbackListener != null)
        {
            callbackListener.onInputDoneClick(view);
        }

        return true;
    }

    private void flashLayout()
    {
        editText.setBackgroundResource(R.drawable.background_password_flash);
        handler.postDelayed(() -> editText.setBackgroundResource(R.drawable.background_password_error), 300);
    }
}

