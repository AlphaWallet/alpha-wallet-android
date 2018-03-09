package com.wallet.crypto.alphawallet.widget;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;

public class AWalletAlertDialog extends Dialog {
    public static final int NONE = 0;
    public static final int SUCCESS = R.drawable.ic_redeemed;
    public static final int NO_SCREENSHOT = R.drawable.ic_no_screenshot;

    private static AWalletAlertDialog dialog = null;
    private ImageView icon;
    private TextView titleText;
    private TextView messageText;
    private Button button;
    private Context context;

    public AWalletAlertDialog(@NonNull Activity activity) {
        super(activity);
        this.context = activity;

        setContentView(R.layout.dialog_awallet_alert);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        icon = findViewById(R.id.dialog_icon);
        titleText = findViewById(R.id.dialog_main_text);
        messageText = findViewById(R.id.dialog_sub_text);
        button = findViewById(R.id.dialog_button1);
    }

    public void setTitle(int resId) {
        titleText.setText(context.getResources().getString(resId));
    }

    public void setButtonText(int resId) {
        button.setText(context.getResources().getString(resId));
    }

    public void setButtonListener(View.OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    public void setMessage(int resId) {
        messageText.setText(context.getResources().getString(resId));
    }

    public void setIcon(int resId) {
        if (resId == NONE) {
            this.icon.setVisibility(View.GONE);
        } else {
            this.icon.setImageResource(resId);
        }
    }
}
