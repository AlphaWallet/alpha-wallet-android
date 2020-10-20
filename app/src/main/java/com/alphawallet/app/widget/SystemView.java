package com.alphawallet.app.widget;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.R;

public class SystemView extends FrameLayout implements View.OnClickListener {
	private ProgressBar progress;
	private View errorBox;
	private TextView messageTxt;
	private View tryAgain;

	private OnClickListener onTryAgainClickListener;
	private FrameLayout emptyBox;

	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;

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
		messageTxt = view.findViewById(R.id.message);
		tryAgain = view.findViewById(R.id.try_again);
		tryAgain.setOnClickListener(this);

		emptyBox = view.findViewById(R.id.empty_box);
	}

	public void attachSwipeRefreshLayout(@Nullable SwipeRefreshLayout swipeRefreshLayout) {
		this.swipeRefreshLayout = swipeRefreshLayout;
	}

	public void attachRecyclerView(@Nullable RecyclerView recyclerView) {
		this.recyclerView = recyclerView;
	}

	public void hide() {
		hideAllComponents();
		setVisibility(GONE);
	}

	private void hideAllComponents() {
		if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
			swipeRefreshLayout.setRefreshing(false);
		}
		emptyBox.setVisibility(GONE);
		errorBox.setVisibility(GONE);
		progress.setVisibility(GONE);
		setVisibility(VISIBLE);
	}

	// Shows the central progress spinner
	public void showCentralSpinner()
	{
		hideAllComponents();
		progress.setVisibility(VISIBLE);
	}

	public void showProgress(boolean shouldShow) {
	    if (shouldShow && swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
	        return;
        }
		if (shouldShow) {
			if (swipeRefreshLayout != null
					&& recyclerView != null
					&& recyclerView.getAdapter() != null
					&& recyclerView.getAdapter().getItemCount() > 0) {
				hide();
				swipeRefreshLayout.setRefreshing(true);
			} else {
				hideAllComponents();
				progress.setVisibility(VISIBLE);
			}
		} else {
			hide();
		}
	}

	public void showSwipe()
	{
		swipeRefreshLayout.setRefreshing(true);
	}

	public void showProgress()
	{
		progress.setVisibility(VISIBLE);
	}

	public void showError(@Nullable String message, @Nullable OnClickListener onTryAgainClickListener) {
		if (recyclerView != null && recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() > 0) {
			hide();
			Snackbar.make(this,
					TextUtils.isEmpty(message) ? getContext().getString(R.string.unknown_error) : message,
					Snackbar.LENGTH_LONG).show();
		} else {
			hideAllComponents();
			errorBox.setVisibility(VISIBLE);
			messageTxt.setText(message);

			this.onTryAgainClickListener = onTryAgainClickListener;

			messageTxt.setVisibility(TextUtils.isEmpty(message) ? GONE : VISIBLE);
			tryAgain.setVisibility(this.onTryAgainClickListener == null ? GONE : VISIBLE);
		}
	}

	public void showEmpty() {
		showEmpty("");
	}

	public void showEmpty(@NonNull String message) {
		showError(message, null);
	}

	public void showEmpty(@LayoutRes int emptyLayout) {
		showEmpty(LayoutInflater.from(getContext())
				.inflate(emptyLayout, emptyBox, false));
	}

	public void showEmpty(View view) {
		hideAllComponents();
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER_VERTICAL;
		view.setLayoutParams(lp);
		emptyBox.setVisibility(VISIBLE);
		emptyBox.removeAllViews();
		emptyBox.addView(view);
	}

	@Override
	public void onClick(View v) {
		if (onTryAgainClickListener != null) {
			hide();
			onTryAgainClickListener.onClick(v);
		}
	}
}
