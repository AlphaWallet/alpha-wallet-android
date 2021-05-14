package com.alphawallet.app.widget;

import android.content.Context;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class TestNetDialog extends BottomSheetDialog {
    private ImageView closeButton;
    private Button confirmButton;

    public TestNetDialog(@NonNull Context context, TestNetDialogCallback callback)
    {
        super(context);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.layout_dialog_testnet_confirmation);
        closeButton = findViewById(R.id.close_action);
        confirmButton = findViewById(R.id.enable_testnet_action);
        setCallback(callback);
    }

    private void setCallback(TestNetDialogCallback listener)
    {
        closeButton.setOnClickListener(v -> listener.onTestNetDialogClosed());
        confirmButton.setOnClickListener(v -> listener.onTestNetDialogConfirmed());
        setOnCancelListener(v -> listener.onTestNetDialogCancelled());
    }

    public interface TestNetDialogCallback {
        void onTestNetDialogClosed();

        void onTestNetDialogConfirmed();

        default void onTestNetDialogCancelled() {}
    }
}
