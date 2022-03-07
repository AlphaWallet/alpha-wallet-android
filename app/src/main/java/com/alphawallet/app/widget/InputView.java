package com.alphawallet.app.widget;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.QRScanning.QRScanner;
import com.alphawallet.app.ui.widget.entity.BoxStatus;
import com.alphawallet.app.util.Utils;

import com.alphawallet.app.R;

import static android.content.Context.CLIPBOARD_SERVICE;

import timber.log.Timber;

public class InputView extends LinearLayout {
    private final Context context;

    private final TextView labelText;
    private final TextView errorText;
    private final TextView statusText;
    private final TextView pasteItem;
    private final EditText editText;
    private final RelativeLayout boxLayout;
    private final ImageButton scanQrIcon;

    private int labelResId;
    private int lines;
    private String inputType;
    private int buttonSrc;
    private String imeOptions;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        inflate(context, R.layout.item_input_view, this);

        labelText = findViewById(R.id.label);
        errorText = findViewById(R.id.error_text);
        editText = findViewById(R.id.edit_text);
        statusText = findViewById(R.id.status_text);
        boxLayout = findViewById(R.id.box_layout);
        scanQrIcon = findViewById(R.id.img_scan_qr);
        pasteItem = findViewById(R.id.text_paste);

        getAttrs(context, attrs);

        bindViews();

        setLines();

        setImeOptions();

        setInputType();
    }

    private void bindViews() {
        labelText.setText(labelResId);
        //Paste
        pasteItem.setOnClickListener(v -> {
            //from clipboard
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            try
            {
                CharSequence textToPaste = clipboard.getPrimaryClip().getItemAt(0).getText();
                editText.setText(textToPaste);
            }
            catch (Exception e)
            {
                Timber.e(e, e.getMessage());
            }
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
            boolean noCam = a.getBoolean(R.styleable.InputView_nocam, true);
            boolean showHeader = a.getBoolean(R.styleable.InputView_show_header, false);
            boolean showPaste = a.getBoolean(R.styleable.InputView_show_paste, false);
            int headerTextId = a.getResourceId(R.styleable.InputView_label, R.string.token_name);
            findViewById(R.id.layout_header).setVisibility(showHeader ? View.VISIBLE : View.GONE);
            TextView headerText = findViewById(R.id.text_header);
            headerText.setText(headerTextId);
            scanQrIcon.setVisibility(noCam ? View.GONE : View.VISIBLE);
            pasteItem.setVisibility(showPaste ? View.VISIBLE : View.GONE);

            if (!noCam)
            {
                scanQrIcon.setOnClickListener(v -> {
                    Intent intent = new Intent(context, QRScanner.class);
                    ((Activity) context).startActivityForResult(intent, C.BARCODE_READER_REQUEST_CODE);
                });
            }
        } finally {
            a.recycle();
        }
    }

    private void setInputType() {
        if (inputType != null) {
            switch (inputType) {
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

    public EditText getEditText() {
        return this.editText;
    }

    public CharSequence getText() {
        return this.editText.getText();
    }

    public void setText(CharSequence text) {
        this.editText.setText(text);
        if (text != null) {
            this.editText.setSelection(text.length());
        }
    }

    public void setError(int resId) {
        if (resId == R.string.empty) {
            errorText.setText(resId);
            errorText.setVisibility(View.GONE);
        } else {
            errorText.setText(resId);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    public void setError(CharSequence message) {
        if (message == null) {
            errorText.setVisibility(View.GONE);
        } else if (message.toString().isEmpty()) {
            errorText.setText(message);
            errorText.setVisibility(View.GONE);
        } else {
            errorText.setText(message);
            errorText.setVisibility(View.VISIBLE);
        }
    }

    public void setStatus(CharSequence statusTxt)
    {
        if (TextUtils.isEmpty(statusTxt))
        {
            statusText.setVisibility(View.GONE);
            statusText.setText(R.string.empty);
            if (errorText.getVisibility() == View.VISIBLE) //cancel error
            {
                setBoxColour(BoxStatus.SELECTED);
            }
        }
        else
        {
            statusText.setText(statusTxt);
            statusText.setVisibility(View.VISIBLE);
        }
    }

    private void setBoxColour(BoxStatus status)
    {
        switch (status)
        {
            case ERROR:
                boxLayout.setBackgroundResource(R.drawable.background_input_error);
                labelText.setTextColor(context.getColor(R.color.danger));
                break;
            case UNSELECTED:
                boxLayout.setBackgroundResource(R.drawable.background_password_entry);
                labelText.setTextColor(context.getColor(R.color.dove));
                errorText.setVisibility(View.GONE);
                break;
            case SELECTED:
                boxLayout.setBackgroundResource(R.drawable.background_input_selected);
                labelText.setTextColor(context.getColor(R.color.azure));
                errorText.setVisibility(View.GONE);
                break;
        }
    }
}
