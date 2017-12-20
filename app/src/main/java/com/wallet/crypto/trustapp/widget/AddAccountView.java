package com.wallet.crypto.trustapp.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.wallet.crypto.trustapp.R;


public class AddAccountView extends FrameLayout implements View.OnClickListener {
	private OnNewAccountClickListener onNewAccountClickListener;
	private OnImportAccountClickListener onImportAccountClickListener;

	public AddAccountView(Context context) {
		this(context, R.layout.layout_dialog_add_account);
	}

	public AddAccountView(Context context, @LayoutRes int layoutId) {
		super(context);

		init(layoutId);
	}

	private void init(@LayoutRes int layoutId) {
		LayoutInflater.from(getContext()).inflate(layoutId, this, true);
		findViewById(R.id.new_account_action).setOnClickListener(this);
		findViewById(R.id.import_account_action).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.new_account_action: {
				if (onNewAccountClickListener != null) {
					onNewAccountClickListener.onNewAccount(view);
				}
			} break;
			case R.id.import_account_action: {
				if (onImportAccountClickListener != null) {
					onImportAccountClickListener.onImportAccount(view);
				}
			} break;
		}
	}
	
	public void setOnNewAccountClickListener(OnNewAccountClickListener onNewAccountClickListener) {
		this.onNewAccountClickListener = onNewAccountClickListener;
	}
	
	public void setOnImportAccountClickListener(OnImportAccountClickListener onImportAccountClickListener) {
		this.onImportAccountClickListener = onImportAccountClickListener;
	}

	public interface OnNewAccountClickListener {
		void onNewAccount(View view);
	}

	public interface OnImportAccountClickListener {
		void onImportAccount(View view);
	}
}
