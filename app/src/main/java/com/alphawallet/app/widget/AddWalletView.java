package com.alphawallet.app.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.alphawallet.app.R;


public class AddWalletView extends FrameLayout implements View.OnClickListener {
	private OnNewWalletClickListener onNewWalletClickListener;
	private OnImportWalletClickListener onImportWalletClickListener;
	private OnWatchWalletClickListener onWatchWalletClickListener;

	public AddWalletView(Context context) {
		this(context, R.layout.layout_dialog_add_account);
	}

	public AddWalletView(Context context, @LayoutRes int layoutId) {
		super(context);

		init(layoutId);
	}

	private void init(@LayoutRes int layoutId) {
		LayoutInflater.from(getContext()).inflate(layoutId, this, true);
		findViewById(R.id.new_account_action).setOnClickListener(this);
		findViewById(R.id.import_account_action).setOnClickListener(this);
		findViewById(R.id.watch_account_action).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.new_account_action: {
				if (onNewWalletClickListener != null) {
					onNewWalletClickListener.onNewWallet();
				}
			} break;
			case R.id.import_account_action: {
				if (onImportWalletClickListener != null) {
					onImportWalletClickListener.onImportWallet();
				}
			} break;
            case R.id.watch_account_action: {
                if (onWatchWalletClickListener != null) {
                    onWatchWalletClickListener.onWatchWallet();
                }
            }
            break;
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

	public interface OnNewWalletClickListener {
		void onNewWallet();
	}

	public interface OnImportWalletClickListener {
		void onImportWallet();
	}

	public interface OnWatchWalletClickListener {
	    void onWatchWallet();
    }
}
