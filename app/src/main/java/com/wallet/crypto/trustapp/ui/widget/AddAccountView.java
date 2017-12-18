package com.wallet.crypto.trustapp.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.wallet.crypto.trustapp.R;


public class AddAccountView extends FrameLayout implements View.OnClickListener {
	private OnNewAccountClickListener onNewAccountClickListener;
	private OnImportAccountClickListener onImportAccountClickListener;

	public AddAccountView(@NonNull Context context) {
		this(context, null);
	}

	public AddAccountView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AddAccountView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.layout_add_account, this, true);
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
