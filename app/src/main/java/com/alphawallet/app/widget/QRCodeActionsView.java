package com.alphawallet.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;

import com.alphawallet.app.R;


public class QRCodeActionsView extends FrameLayout implements View.OnClickListener {
    private OnClickListener onSendToAddressClickListener;
    private OnClickListener onAddCustonTokenClickListener;
    private OnClickListener onWatchWalletClickListener;
    private OnClickListener onOpenInEtherscanClickListener;
    private OnClickListener onCloseActionListener;

    public QRCodeActionsView(Context context) {
        this(context, R.layout.layout_dialog_qr_code_actions);
    }

    public QRCodeActionsView(Context context, @LayoutRes int layoutId) {
        super(context);

        init(layoutId);
    }

    private void init(@LayoutRes int layoutId) {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.send_to_this_address_action).setOnClickListener(this);
        findViewById(R.id.add_custom_token_action).setOnClickListener(this);
        findViewById(R.id.watch_account_action).setOnClickListener(this);
        findViewById(R.id.open_in_etherscan_action).setOnClickListener(this);
        findViewById(R.id.close_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.send_to_this_address_action)
        {
            if (onSendToAddressClickListener != null) {
                onSendToAddressClickListener.onClick(view);
            }
        }
        else if (view.getId() == R.id.add_custom_token_action)
        {
            if (onAddCustonTokenClickListener != null) {
                onAddCustonTokenClickListener.onClick(view);
            }
        }
        else if (view.getId() == R.id.watch_account_action)
        {
            if (onWatchWalletClickListener != null) {
                onWatchWalletClickListener.onClick(view);
            }
        }
        else if (view.getId() == R.id.open_in_etherscan_action)
        {
            if (onOpenInEtherscanClickListener != null) {
                onOpenInEtherscanClickListener.onClick(view);
            }
        }
        else if (view.getId() == R.id.close_action)
        {
            if (onCloseActionListener != null) {
                onCloseActionListener.onClick(view);
            }
        }
    }

    public void setOnSendToAddressClickListener(OnClickListener onSendToAddressClickListener) {
        this.onSendToAddressClickListener = onSendToAddressClickListener;
    }

    public void setOnAddCustonTokenClickListener(OnClickListener onAddCustonTokenClickListener) {
        this.onAddCustonTokenClickListener = onAddCustonTokenClickListener;
    }

    public void setOnWatchWalletClickListener(OnClickListener onWatchWalletClickListener) {
        this.onWatchWalletClickListener = onWatchWalletClickListener;
    }

    public void setOnOpenInEtherscanClickListener(OnClickListener onOpenInEtherscanClickListener) {
        this.onOpenInEtherscanClickListener = onOpenInEtherscanClickListener;
    }

    public void setOnCloseActionListener(OnClickListener onCloseActionListener) {
        this.onCloseActionListener = onCloseActionListener;
    }
}
