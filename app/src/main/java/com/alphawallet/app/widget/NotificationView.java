package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.google.android.material.card.MaterialCardView;

public class NotificationView extends LinearLayout {
    private MaterialCardView layout;
    private TextView title;
    private TextView message;
    private Button primaryButton;
    private Button secondaryButton;
    private int backgroundColorRes;
    private int textColorRes;

    public static class Builder {
        private final Context context;
        private int backgroundColorRes = -1;
        private int textColorRes = -1;
        private int titleRes = -1;
        private int messageRes = -1;
        private int primaryButtonTextRes = -1;
        private int secondaryButtonTextRes = -1;
        private OnPrimaryButtonClickListener primaryButtonClickListener;
        private OnSecondaryButtonClickListener secondaryButtonClickListener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(int titleRes) {
            this.titleRes = titleRes;
            return this;
        }

        public Builder setMessage(int messageRes) {
            this.messageRes = messageRes;
            return this;
        }

        public Builder setBackgroundColor(int backgroundColorRes) {
            this.backgroundColorRes = backgroundColorRes;
            return this;
        }

        public Builder setTextColor(int textColorRes) {
            this.textColorRes = textColorRes;
            return this;
        }

        public Builder setPrimaryButtonText(int primaryButtonTextRes) {
            this.primaryButtonTextRes = primaryButtonTextRes;
            return this;
        }

        public Builder setSecondaryButtonText(int secondaryButtonTextRes) {
            this.secondaryButtonTextRes = secondaryButtonTextRes;
            return this;
        }

        public Builder setPrimaryButtonClickListener(OnPrimaryButtonClickListener listener) {
            this.primaryButtonClickListener = listener;
            return this;
        }

        public Builder setSecondaryButtonClickListener(OnSecondaryButtonClickListener listener) {
            this.secondaryButtonClickListener = listener;
            return this;
        }

        public NotificationView build() {
            NotificationView view = new NotificationView(context);
            view.setTitle(context.getString(titleRes));
            view.setMessage(context.getString(messageRes));
            view.setNotificationBackgroundColor(backgroundColorRes);
            view.setNotificationTextColor(textColorRes);
            if (primaryButtonTextRes != -1) {
                view.setPrimaryButtonText(context.getString(primaryButtonTextRes));
                view.setPrimaryButtonListener(primaryButtonClickListener);
            }

            if (secondaryButtonTextRes != -1) {
                view.setSecondaryButtonText(context.getString(secondaryButtonTextRes));
                view.setSecondaryButtonListener(secondaryButtonClickListener);
            }
            return view;
        }
    }

    public interface OnPrimaryButtonClickListener {
        void onPrimaryButtonClicked();
    }

    public interface OnSecondaryButtonClickListener {
        void onSecondaryButtonClicked();
    }

    public NotificationView(Context context) {
        this(context, null);
    }

    public NotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.layout_notification_view, this);

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        initializeViews();

        processAttrs(context, attrs);
    }

    private void processAttrs(Context context, AttributeSet attrs) {
        if (attrs != null) {
            getAttrs(context, attrs);
            if (backgroundColorRes != -1) {
                setNotificationBackgroundColor(backgroundColorRes);
            }

            if (textColorRes != -1) {
                setNotificationTextColor(textColorRes);
            }
        }
    }

    private void initializeViews() {
        layout = findViewById(R.id.layout);
        title = findViewById(R.id.title);
        message = findViewById(R.id.message);
        primaryButton = findViewById(R.id.btn_primary);
        secondaryButton = findViewById(R.id.btn_secondary);
    }

    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.NotificationView,
                0, 0
        );

        try {
            backgroundColorRes = a.getResourceId(R.styleable.NotificationView_backgroundColour, -1);
            textColorRes = a.getResourceId(R.styleable.NotificationView_textColor, -1);
        } finally {
            a.recycle();
        }
    }

    public void setPrimaryButtonListener(OnPrimaryButtonClickListener listener) {
        if (listener != null) {
            primaryButton.setOnClickListener(v -> {
                listener.onPrimaryButtonClicked();
            });
        }
    }

    public void setSecondaryButtonListener(OnSecondaryButtonClickListener listener) {
        if (listener != null) {
            secondaryButton.setOnClickListener(v -> {
                listener.onSecondaryButtonClicked();
            });
        }
    }

    public void setTitle(String titleText) {
        title.setText(titleText);
    }

    public void setMessage(String messageText) {
        message.setText(messageText);
    }

    public void setPrimaryButtonText(String primaryButtonText) {
        if (!primaryButtonText.isEmpty()) {
            primaryButton.setVisibility(View.VISIBLE);
            primaryButton.setText(primaryButtonText);
        } else {
            primaryButton.setVisibility(View.GONE);
        }
    }

    public void setSecondaryButtonText(String secondaryButtonText) {
        if (!secondaryButtonText.isEmpty()) {
            secondaryButton.setVisibility(View.VISIBLE);
            secondaryButton.setText(secondaryButtonText);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }
    }

    public void setNotificationBackgroundColor(int backgroundColorRes) {
        layout.setCardBackgroundColor(backgroundColorRes);
    }

    public void setNotificationTextColor(int textColorRes) {
        title.setTextColor(textColorRes);
        message.setTextColor(textColorRes);
        primaryButton.setTextColor(textColorRes);
        secondaryButton.setTextColor(textColorRes);
    }
}

