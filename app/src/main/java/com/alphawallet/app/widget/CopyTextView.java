package com.alphawallet.app.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alphawallet.app.R;

public class CopyTextView extends LinearLayout {

    public static final String KEY_ADDRESS = "key_address";

    private Context context;
    private ImageView copy;
    private TextView text;

    private int textResId;
    private boolean showToast;

    public CopyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        inflate(context, R.layout.item_copy_textview, this);

        getAttrs(context, attrs);

        bindViews();
    }

    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.InputView,
                0, 0
        );

        try {
            textResId = a.getResourceId(R.styleable.CopyTextView_text, R.string.empty);
            showToast = a.getBoolean(R.styleable.CopyTextView_showToast, true);
        } finally {
            a.recycle();
        }
    }

    private void bindViews() {
        copy = findViewById(R.id.img_copy);
        text = findViewById(R.id.text);
        text.setText(textResId);

        copy.setOnClickListener(v -> copyToClipboard());
    }

    public String getText() {
        return this.text.getText().toString();
    }

    public void setText(CharSequence text) {
        this.text.setText(text);
    }

    private void copyToClipboard()
    {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, text.getText().toString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }

        if(showToast) Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }
}
