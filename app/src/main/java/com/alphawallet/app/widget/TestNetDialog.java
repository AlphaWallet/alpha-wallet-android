package com.alphawallet.app.widget;

import android.content.Context;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class TestNetDialog extends BottomSheetDialog {
    private final ImageView closeButton;
    private final Button confirmButton;
    private final long newChainId;

    public TestNetDialog(@NonNull Context context, long chainId, TestNetDialogCallback callback)
    {
        super(context);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.layout_dialog_testnet_confirmation);
        closeButton = findViewById(R.id.close_action);
        confirmButton = findViewById(R.id.enable_testnet_action);
        newChainId = chainId;
        setCallback(callback);
    }

    private void setCallback(TestNetDialogCallback listener)
    {
        closeButton.setOnClickListener(v -> {
            listener.onTestNetDialogClosed();
            dismiss();
        });
        confirmButton.setOnClickListener(v -> {
            listener.onTestNetDialogConfirmed(newChainId);
            dismiss();
        });
        setOnCancelListener(v -> listener.onTestNetDialogCancelled());
    }

    public interface TestNetDialogCallback {
        void onTestNetDialogClosed();

        void onTestNetDialogConfirmed(long chainId);

        default void onTestNetDialogCancelled() {}
    }
}
