package com.alphawallet.app.widget;

import android.Manifest;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public class PermissionRationaleDialog extends BottomSheetDialog
{
    private final TextView title;
    private final TextView body;
    private final MaterialButton okButton;
    private final MaterialButton cancelButton;

    public PermissionRationaleDialog(
        @NonNull Context context,
        String permission,
        @NonNull View.OnClickListener okListener,
        @NonNull View.OnClickListener cancelListener
    )
    {
        super(context);
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.dialog_permission_rationale);
        title = findViewById(R.id.title);
        body = findViewById(R.id.body);
        okButton = findViewById(R.id.btn_ok);
        cancelButton = findViewById(R.id.btn_cancel);

        if (title != null)
        {
            title.setText(getTitle(permission));
        }

        if (body != null)
        {
            body.setText(getBody(permission));
        }

        if (okButton != null)
        {
            okButton.setText(getOkButtonText(permission));
        }

        if (cancelButton != null)
        {
            cancelButton.setText(getCancelButtonText(permission));
        }

        if (okButton != null)
        {
            okButton.setOnClickListener(v ->
            {
                okListener.onClick(v);
                dismiss();
            });
        }

        if (cancelButton != null)
        {
            cancelButton.setOnClickListener(v ->
            {
                cancelListener.onClick(v);
                dismiss();
            });
        }
    }

    public static void show(@NonNull Context context,
                            String permission,
                            View.OnClickListener okListener,
                            View.OnClickListener cancelListener)
    {
        PermissionRationaleDialog dialog
            = new PermissionRationaleDialog(context, permission, okListener, cancelListener);
        dialog.show();
    }

    private String getOkButtonText(String permission)
    {
        switch (permission)
        {
            case Manifest.permission.POST_NOTIFICATIONS:
                return getContext().getString(R.string.btn_ok_request_post_notifications_permission);
            default:
                return "";
        }
    }

    private String getCancelButtonText(String permission)
    {
        return getContext().getString(R.string.btn_skip);
    }

    private String getBody(String permission)
    {
        switch (permission)
        {
            case Manifest.permission.POST_NOTIFICATIONS:
                return getContext().getString(R.string.body_request_post_notifications_permission);
            default:
                return "";
        }
    }

    private String getTitle(String permission)
    {
        switch (permission)
        {
            case Manifest.permission.POST_NOTIFICATIONS:
                return getContext().getString(R.string.title_request_post_notifications_permission);
            default:
                return "";
        }
    }
}
