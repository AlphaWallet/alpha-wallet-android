package com.wallet.crypto.trustapp.ui.widget.holder;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BinderViewHolder<T> extends RecyclerView.ViewHolder {
	public BinderViewHolder(int resId, ViewGroup parent) {
		super(LayoutInflater.from(parent.getContext())
				.inflate(resId, parent, false));
	}

	public abstract void bind(@Nullable T data, @NonNull Bundle addition);

	public void bind(@Nullable T data) {
		bind(data, Bundle.EMPTY);
	}

	protected <T extends View> T findViewById(int id) {
		return itemView.findViewById(id);
	}

	protected Context getContext() {
		return itemView.getContext();
	}

	protected String getString(int stringResId) {
		return getContext().getString(stringResId);
	}


	public String getString(int stringResId, Object... args) {
		return getContext().getString(stringResId, args);
	}
}
