package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.util.Utils;

import com.alphawallet.app.R;

public class PasswordInputView extends LinearLayout implements TextView.OnEditorActionListener
{
    private final Context context;

    private TextView label;
    private TextView error;
    private EditText editText;
    private CheckBox togglePassword;
    private TextView instruction;
    private RelativeLayout container;

    private int labelResId;
    private int lines;
    private String inputType;
    private int minHeight;
    private int innerPadding;
    private String imeOptions;
    private Activity activity;
    private LayoutCallbackListener callbackListener;
    private View bottomMarker;

    public PasswordInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        getAttrs(context, attrs);

        bindViews();

        setLines();

        setImeOptions();

        setInputType();
        setMinHeight();
    }

    public void setLayoutListener(Activity a, LayoutCallbackListener callback, View bm)
    {
        activity = a;
        callbackListener = callback;
        getViewTreeObserver().addOnGlobalLayoutListener(screenLayoutListener);
        getEditText().setOnEditorActionListener(this);

        bottomMarker = bm;
    }

    public EditText getEditText()
    {
        return editText;
    }

    private void bindViews() {
        inflate(context, R.layout.layout_password_input, this);

        label = findViewById(R.id.label);
        label.setText(labelResId);
        error = findViewById(R.id.error);
        editText = findViewById(R.id.edit_text);
        container = findViewById(R.id.box_layout);
        instruction = findViewById(R.id.instruction);
        if (labelResId != R.string.empty) label.setVisibility(View.VISIBLE);
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
            minHeight = a.getInteger(R.styleable.InputView_minHeight, 0);
            innerPadding = a.getInteger(R.styleable.InputView_innerPadding, 0);
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
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        } else {
            error.setText(resId);
            error.setVisibility(View.VISIBLE);
            editText.setBackgroundResource(R.drawable.background_password_error);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.warning_red));
        }
    }

    public void setError(CharSequence message) {
        if (message == null) {
            error.setVisibility(View.GONE);
            editText.setBackgroundResource(R.drawable.background_password_entry);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        } else if (message.toString().isEmpty()) {
            error.setText(message);
            error.setVisibility(View.GONE);
            editText.setBackgroundResource(R.drawable.background_password_entry);
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
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

    private ViewTreeObserver.OnGlobalLayoutListener screenLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout()
        {
            if (activity == null || callbackListener == null || bottomMarker == null) return;

            int contentViewBottom = activity.getWindow()
                    .findViewById(Window.ID_ANDROID_CONTENT)
                    .getBottom();

            if (contentViewBottom == 0) return;

            int[] location = new int[2];
            bottomMarker.getLocationOnScreen(location);
            int actionLocation = location[1];

            if (actionLocation < (contentViewBottom - 200))
            {
                callbackListener.onLayoutShrunk();
            }
            else
            {
                callbackListener.onLayoutExpand();
            }
        }
    };

    @Override
    public boolean onEditorAction(TextView view, int i, KeyEvent keyEvent)
    {
        if (callbackListener != null) callbackListener.onInputDoneClick(view);
        return true;
    }
}

