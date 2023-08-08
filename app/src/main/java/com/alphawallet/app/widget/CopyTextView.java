package com.alphawallet.app.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.alphawallet.app.R;
import com.alphawallet.app.util.Utils;
import com.google.android.material.button.MaterialButton;

public class CopyTextView extends LinearLayout
{
    public static final String KEY_ADDRESS = "key_address";
    private final Context context;
    private MaterialButton button;
    private int textResId;
    private int gravity;
    private int lines;
    private boolean showToast;
    private boolean boldFont;
    private boolean removePadding;
    private float marginRight;
    private String originalText;

    public CopyTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;

        inflate(context, R.layout.item_copy_textview, this);

        getAttrs(context, attrs);

        bindViews();
    }

    private void getAttrs(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
            attrs,
            R.styleable.CopyTextView,
            0, 0
        );

        try
        {
            textResId = a.getResourceId(R.styleable.CopyTextView_text, R.string.action_add_wallet);
            gravity = a.getInt(R.styleable.CopyTextView_android_gravity, Gravity.NO_GRAVITY);
            showToast = a.getBoolean(R.styleable.CopyTextView_showToast, true);
            boldFont = a.getBoolean(R.styleable.CopyTextView_bold, false);
            removePadding = a.getBoolean(R.styleable.CopyTextView_removePadding, false);
            marginRight = a.getDimension(R.styleable.CopyTextView_marginRight, 0.0f);
            lines = a.getInt(R.styleable.CopyTextView_lines, 1);
        }
        finally
        {
            a.recycle();
        }
    }

    private void bindViews()
    {
        if (lines == 2)
        {
            button = findViewById(R.id.button_address);
            findViewById(R.id.button).setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
        }
        else
        {
            button = findViewById(R.id.button);
        }

        setText(getContext().getString(textResId));
        button.setOnClickListener(v -> copyToClipboard());
    }

    public String getText()
    {
        return originalText;
    }

    public void setFixedText(CharSequence text)
    {
        originalText = text.toString();

        setVisibility(TextUtils.isEmpty(originalText) ? View.GONE : View.VISIBLE);

        if (Utils.isAddressValid(originalText))
        {
            button.setText(Utils.splitAddress(originalText));
        }
        else if (Utils.isTxHashValid(originalText))
        {
            button.setText(Utils.formatTxHash(originalText, 10));
        }
        else
        {
            button.setText(originalText);
        }
    }

    public void setText(CharSequence text)
    {
        originalText = text.toString();

        setVisibility(TextUtils.isEmpty(originalText) ? View.GONE : View.VISIBLE);

        if (Utils.isAddressValid(originalText))
        {
            button.setText(Utils.formatAddress(originalText, 10));
        }
        else if (Utils.isTxHashValid(originalText))
        {
            button.setText(Utils.formatTxHash(originalText, 10));
        }
        else
        {
            button.setText(originalText);
        }
    }

    private void copyToClipboard()
    {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(KEY_ADDRESS, originalText);
        if (clipboard != null)
        {
            clipboard.setPrimaryClip(clip);
        }

        if (showToast)
        {
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }
}
