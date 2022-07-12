package com.alphawallet.app.widget;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class AWBottomSheetDialog extends BottomSheetDialog {
    private final ImageView closeButton;
    private final Button confirmButton;
    private final TextView contentTextView;
    private final TextView titleTextView;

    public AWBottomSheetDialog(@NonNull Context context, Callback callback)
    {
        super(context);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.layout_dialog_common);

        titleTextView = findViewById(R.id.title);
        contentTextView = findViewById(R.id.content);
        closeButton = findViewById(R.id.close_action);
        confirmButton = findViewById(R.id.button_confirm);
        setCallback(callback);
    }

    private void setCallback(Callback listener)
    {
        closeButton.setOnClickListener(v -> {
            listener.onClosed();
            dismiss();
        });
        confirmButton.setOnClickListener(v -> {
            listener.onConfirmed();
            dismiss();
        });
        setOnCancelListener(v -> listener.onCancelled());
    }

    public void setContent(String text)
    {
        contentTextView.setText(text);
    }

    public void setConfirmButton(String text)
    {
        confirmButton.setText(text);
    }

    @Override
    public void setTitle(CharSequence title)
    {
        titleTextView.setText(title);
    }

    public interface Callback
    {
        void onClosed();
        void onConfirmed();
        void onCancelled();
    }
}
