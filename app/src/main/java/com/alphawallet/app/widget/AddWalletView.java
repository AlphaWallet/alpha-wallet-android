package com.alphawallet.app.widget;

import android.content.Context;
import androidx.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.alphawallet.app.R;


public class AddWalletView extends FrameLayout implements View.OnClickListener {
    private OnNewWalletClickListener onNewWalletClickListener;
    private OnImportWalletClickListener onImportWalletClickListener;
    private OnWatchWalletClickListener onWatchWalletClickListener;
    private OnCloseActionListener onCloseActionListener;

    public AddWalletView(Context context) {
        this(context, R.layout.layout_dialog_add_account);
    }

    public AddWalletView(Context context, @LayoutRes int layoutId) {
        super(context);

        init(layoutId);
    }

    private void init(@LayoutRes int layoutId) {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.close_action).setOnClickListener(this);
        findViewById(R.id.new_account_action).setOnClickListener(this);
        findViewById(R.id.import_account_action).setOnClickListener(this);
        findViewById(R.id.watch_account_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        final int close_action = R.id.close_action;
        final int new_account_action = R.id.new_account_action;
        final int import_account_action = R.id.import_account_action;
        final int watch_account_action = R.id.watch_account_action;

        switch (view.getId()) {
            case close_action: {
                if (onCloseActionListener != null) {
                    onCloseActionListener.onClose(view);
                }
                break;
            }
            case new_account_action: {
                if (onNewWalletClickListener != null) {
                    onNewWalletClickListener.onNewWallet(view);
                }
                break;
            }
            case import_account_action: {
                if (onImportWalletClickListener != null) {
                    onImportWalletClickListener.onImportWallet(view);
                }
                break;
            }
            case watch_account_action: {
                if (onWatchWalletClickListener != null) {
                    onWatchWalletClickListener.onWatchWallet(view);
                }
                break;
            }
        }
    }

    public void setOnNewWalletClickListener(OnNewWalletClickListener onNewWalletClickListener) {
        this.onNewWalletClickListener = onNewWalletClickListener;
    }

    public void setOnImportWalletClickListener(OnImportWalletClickListener onImportWalletClickListener) {
        this.onImportWalletClickListener = onImportWalletClickListener;
    }

    public void setOnWatchWalletClickListener(OnWatchWalletClickListener onWatchWalletClickListener) {
        this.onWatchWalletClickListener = onWatchWalletClickListener;
    }

    public void setOnCloseActionListener(OnCloseActionListener onCloseActionListener) {
        this.onCloseActionListener = onCloseActionListener;
    }

    public interface OnNewWalletClickListener {
        void onNewWallet(View view);
    }

    public interface OnImportWalletClickListener {
        void onImportWallet(View view);
    }

    public interface OnWatchWalletClickListener {
        void onWatchWallet(View view);
    }

    public interface OnCloseActionListener {
        void onClose(View view);
    }
}
