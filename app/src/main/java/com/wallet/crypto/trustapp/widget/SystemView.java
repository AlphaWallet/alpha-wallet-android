package com.wallet.crypto.trustapp.widget;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;

public class SystemView extends FrameLayout implements View.OnClickListener {
	private ProgressBar progress;
	private View errorBox;
	private TextView message;
	private View tryAgain;

	private OnClickListener onTryAgain;
	private FrameLayout emptyBox;

	public SystemView(@NonNull Context context) {
		super(context);
	}

	public SystemView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public SystemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_system_view, this, false);
		addView(view);
		progress = view.findViewById(R.id.progress);

		errorBox = view.findViewById(R.id.error_box);
		message = view.findViewById(R.id.message);
		tryAgain = view.findViewById(R.id.try_again);
		tryAgain.setOnClickListener(this);

		emptyBox = view.findViewById(R.id.empty_box);
	}

	public void showProgress(boolean shouldShow) {
		if (shouldShow) {
			showProgress();
		} else {
			hideProgress();
		}
	}

	public void showProgress() {
		setVisibility(VISIBLE);
		errorBox.setVisibility(GONE);
		progress.setVisibility(VISIBLE);
	}

	public void hideProgress() {
		errorBox.setVisibility(GONE);
		progress.setVisibility(GONE);
		setVisibility(GONE);
	}

	public void showError(@Nullable String message, @Nullable OnClickListener onTryAgain) {
		hideProgress();
		setVisibility(VISIBLE);
		errorBox.setVisibility(VISIBLE);

		if (TextUtils.isEmpty(message)) {
			this.message.setVisibility(GONE);
		} else {
			this.message.setVisibility(VISIBLE);
			this.message.setText(message);
		}
		this.onTryAgain = onTryAgain;
		tryAgain.setVisibility(this.onTryAgain == null ? GONE : VISIBLE);
	}

	public void hideError() {
		setVisibility(GONE);
		progress.setVisibility(GONE);
		errorBox.setVisibility(GONE);
	}

	public void showEmpty() {
		showEmpty("");
	}

	public void showEmpty(String message) {
		hideProgress();
		showError(message, null);
	}

	public void showEmpty(@LayoutRes int emptyLayout) {
		LayoutInflater.from(getContext())
				.inflate(emptyLayout, emptyBox, true);
	}

	@Override
	public void onClick(View v) {
		if (onTryAgain != null) {
			hideError();
			onTryAgain.onClick(v);
		}
	}
}
