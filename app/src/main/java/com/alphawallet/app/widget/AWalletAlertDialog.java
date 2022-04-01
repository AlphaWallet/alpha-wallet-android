package com.alphawallet.app.widget;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;

public class AWalletAlertDialog extends Dialog {
    public static final int NONE = 0;
    public static final int SUCCESS = R.drawable.ic_redeemed;
    public static final int ERROR = R.drawable.ic_error;
    public static final int NO_SCREENSHOT = R.drawable.ic_no_screenshot;
    public static final int WARNING = R.drawable.ic_warning;

    public enum TEXT_STYLE
    {
        CENTERED,
        LEFT
    }

    private final ImageView icon;
    private final TextView titleText;
    private final TextView messageText;
    private final Button button;
    private final Button secondaryButton;
    private final Context context;
    private final ProgressBar progressBar;
    private final RelativeLayout viewContainer;
    private final RelativeLayout dialogLayout;

    public AWalletAlertDialog(@NonNull Context context) {
        super(context);
        this.context = context;

        setContentView(R.layout.dialog_awallet_alert);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        icon = findViewById(R.id.dialog_icon);
        titleText = findViewById(R.id.dialog_main_text);
        messageText = findViewById(R.id.dialog_sub_text);
        button = findViewById(R.id.dialog_button1);
        secondaryButton = findViewById(R.id.dialog_button2);
        progressBar = findViewById(R.id.dialog_progress);
        viewContainer = findViewById(R.id.dialog_view);
        dialogLayout = findViewById(R.id.layout_dialog_container);

        button.setOnClickListener(v -> dismiss());
        secondaryButton.setOnClickListener(v -> dismiss());
    }

    public AWalletAlertDialog(@NonNull Context context, int iconRes)
    {
        this(context);
        setIcon(iconRes);
    }

    public void makeWide()
    {
        float scale = context.getResources().getDisplayMetrics().density;
        int dp15 = (int) (15*scale + 0.5f);
        int dp10 = (int) (10*scale + 0.5f);
        dialogLayout.setPadding(dp15, dp15, dp15, dp15);
        ViewGroup.MarginLayoutParams marginLayout = (ViewGroup.MarginLayoutParams) dialogLayout.getLayoutParams();
        marginLayout.setMargins(dp10, dp10, dp10, dp10);
        dialogLayout.requestLayout();
    }

    public void setProgressMode() {
        icon.setVisibility(View.GONE);
        messageText.setVisibility(View.GONE);
        button.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void setTitle(int resId) {
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(context.getResources().getString(resId));
    }

    @Override
    public void setTitle(CharSequence message) {
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(message);
    }

    public void setButton(int resId, View.OnClickListener listener)
    {
        setButtonText(resId);
        setButtonListener(listener);
    }

    public void setButtonText(int resId) {
        button.setVisibility(View.VISIBLE);
        button.setText(context.getResources().getString(resId));
    }

    public void setButtonListener(View.OnClickListener listener) {
        button.setOnClickListener(listener);
    }

    public void setSecondaryButton(int resId, View.OnClickListener listener)
    {
        setSecondaryButtonText(resId);
        setSecondaryButtonListener(listener);
    }

    public void setSecondaryButtonText(int resId) {
        secondaryButton.setVisibility(View.VISIBLE);
        secondaryButton.setText(context.getResources().getString(resId));
    }

    public void setSecondaryButtonListener(View.OnClickListener listener) {
        secondaryButton.setOnClickListener(listener);
    }

    public void setMessage(int resId) {
        messageText.setVisibility(View.VISIBLE);
        messageText.setText(context.getResources().getString(resId));
    }

    public void setMessage(CharSequence message) {
        messageText.setVisibility(View.VISIBLE);
        messageText.setText(message);
    }

    public void setMessage(String message) {
        messageText.setVisibility(View.VISIBLE);
        messageText.setText(message);
    }

    public void setIcon(int resId) {
        if (resId == NONE) {
            this.icon.setVisibility(View.GONE);
        } else {
            this.icon.setVisibility(View.VISIBLE);
            this.icon.setImageResource(resId);
        }
    }

    public void setView(View view) {
        viewContainer.addView(view);
    }

    public void setTextStyle(TEXT_STYLE style)
    {
        switch (style)
        {
            case CENTERED:
                messageText.setGravity(Gravity.CENTER_HORIZONTAL);
                break;
            case LEFT:
                messageText.setGravity(Gravity.START);
                break;
        }
    }
}
