package com.alphawallet.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;

import com.alphawallet.app.R;


public class WalletFragmentActionsView extends FrameLayout implements View.OnClickListener {
    private OnClickListener onCopyWalletAddressClickListener;
    private OnClickListener onShowMyWalletAddressClickListener;
    private OnClickListener onAddHideTokensClickListener;
    private OnClickListener onRenameThisWalletListener;

    public WalletFragmentActionsView(Context context) {
        this(context, R.layout.layout_dialog_wallet_actions);
    }

    public WalletFragmentActionsView(Context context, @LayoutRes int layoutId) {
        super(context);

        init(layoutId);
    }

    private void init(@LayoutRes int layoutId) {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        findViewById(R.id.copy_wallet_address_action).setOnClickListener(this);
        findViewById(R.id.show_my_wallet_address_action).setOnClickListener(this);
        findViewById(R.id.add_hide_tokens_action).setOnClickListener(this);
        findViewById(R.id.rename_this_wallet_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.copy_wallet_address_action: {
                if (onCopyWalletAddressClickListener != null) {
                    onCopyWalletAddressClickListener.onClick(view);
                }
                break;
            }
            case R.id.show_my_wallet_address_action: {
                if (onShowMyWalletAddressClickListener != null) {
                    onShowMyWalletAddressClickListener.onClick(view);
                }
                break;
            }
            case R.id.add_hide_tokens_action: {
                if (onAddHideTokensClickListener != null) {
                    onAddHideTokensClickListener.onClick(view);
                }
                break;
            }
            case R.id.rename_this_wallet_action: {
                if (onRenameThisWalletListener != null) {
                    onRenameThisWalletListener.onClick(view);
                }
            }
        }
    }

    public void setOnCopyWalletAddressClickListener(OnClickListener onClickListener) {
        this.onCopyWalletAddressClickListener = onClickListener;
    }

    public void setOnShowMyWalletAddressClickListener(OnClickListener onClickListener) {
        this.onShowMyWalletAddressClickListener = onClickListener;
    }

    public void setOnAddHideTokensClickListener(OnClickListener onClickListener) {
        this.onAddHideTokensClickListener = onClickListener;
    }

    public void setOnRenameThisWalletClickListener(OnClickListener onClickListener) {
        this.onRenameThisWalletListener = onClickListener;
    }
}
